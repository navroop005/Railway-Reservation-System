CREATE OR REPLACE PROCEDURE add_journey_info(
    train_id INTEGER, 
    doj DATE, 
    num_ac INTEGER, 
    num_sl INTEGER
)
LANGUAGE plpgsql
AS $$
    DECLARE
        berth_ac INTEGER;
        berth_sl INTEGER;
    BEGIN
        berth_ac := (
            SELECT c.value
            FROM coach_info c
            WHERE c.item = 'berths_per_ac'
        );
        berth_sl := (
            SELECT c.value
            FROM coach_info c
            WHERE c.item = 'berths_per_sl'
        );

        INSERT INTO journey VALUES (train_id, doj, num_ac, num_sl, num_ac * berth_ac, num_sl * berth_sl);

        EXCEPTION
            WHEN foreign_key_violation THEN
                INSERT INTO trains VALUES (train_id);
                INSERT INTO journey VALUES (train_id, doj, num_ac, num_sl, num_ac * berth_ac, num_sl * berth_sl);

                RAISE NOTICE 'New train_id added.';
    END;
$$;


CREATE OR REPLACE FUNCTION add_passenger_info(
    inp_name VARCHAR
)
RETURNS INTEGER
LANGUAGE PLPGSQL
AS $$
    DECLARE
        gen_passenger_id INTEGER;
    BEGIN
        gen_passenger_id := floor(random()*1000000);
        INSERT INTO passenger(passenger_id, name) VALUES (gen_passenger_id, inp_name);

        RETURN gen_passenger_id;
        
        EXCEPTION
            WHEN unique_violation THEN
                gen_passenger_id := add_passenger_info(inp_name);
                RETURN gen_passenger_id;

    END;
$$;

CREATE OR REPLACE FUNCTION update_availability(
    inp_train_id INTEGER, 
    inp_doj DATE, 
    coach_type CHAR(2), 
    num_passenger INTEGER
)
RETURNS INTEGER
LANGUAGE PLPGSQL
AS $$
    DECLARE
        avl_seat INTEGER;
    BEGIN
        IF coach_type = 'AC' THEN
            avl_seat := (
                SELECT j.available_ac_berths
                FROM journey j
                WHERE j.train_id = inp_train_id AND j.doj = inp_doj
                FOR UPDATE
            ) ;
        ELSE
            avl_seat := (
                SELECT j.available_sl_berths
                FROM journey j
                WHERE j.train_id = inp_train_id AND j.doj = inp_doj
                FOR UPDATE
            );
        END IF;

        IF avl_seat >= num_passenger THEN
            avl_seat := avl_seat - num_passenger;
            IF coach_type = 'AC' THEN
                UPDATE journey j SET available_ac_berths = avl_seat WHERE j.train_id = inp_train_id AND j.doj = inp_doj;
            ELSE
                UPDATE journey j SET available_sl_berths = avl_seat WHERE j.train_id = inp_train_id AND j.doj = inp_doj;
            END IF;
            RETURN avl_seat;
        ELSE 
            RETURN -1;
        END IF;
    END;
$$;

CREATE OR REPLACE FUNCTION book_ticket(
    inp_train_id INTEGER, 
    inp_doj DATE, 
    inp_num_passenger INTEGER, 
    inp_coach_type CHAR(2), 
    inp_passenger_list INTEGER[],
    start_seat_num INTEGER
)
RETURNS INTEGER
LANGUAGE PLPGSQL
AS $$
    DECLARE
        p_id INTEGER;
        generated_pnr INTEGER;
        berth_no INTEGER;
        coach_no INTEGER;
        berth_per_coach INTEGER;
    BEGIN
        generated_pnr := random()*1000000;
        INSERT INTO tickets (pnr, train_id, doj, number_of_passengers, coach_type) VALUES (generated_pnr, inp_train_id, inp_doj, inp_num_passenger, inp_coach_type);

        IF inp_coach_type = 'AC' THEN
            berth_per_coach := (
                SELECT c.value
                FROM coach_info c
                WHERE c.item = 'berths_per_ac'
            );
        ELSE
            berth_per_coach := (
                SELECT c.value
                FROM coach_info c
                WHERE c.item = 'berths_per_sl'
            );
        END IF;

        FOREACH p_id IN ARRAY inp_passenger_list
        LOOP
            berth_no := start_seat_num % berth_per_coach + 1;
            coach_no := start_seat_num / berth_per_coach + 1;

            INSERT INTO booked values(p_id, generated_pnr, berth_no, coach_no);

            start_seat_num := start_seat_num+1;
        END LOOP;

        RETURN generated_pnr;

        EXCEPTION
            WHEN unique_violation THEN
                generated_pnr := book_ticket(inp_train_id,inp_doj,inp_num_passenger,inp_coach_type,inp_passenger_list,start_seat_num); 
                RETURN generated_pnr;
    END;
$$;

