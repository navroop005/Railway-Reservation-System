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
            RAISE NOTICE 'Train_id invalid';
    END;
$$;


CREATE OR REPLACE FUNCTION add_passenger_info(
    inp_name VARCHAR, 
    inp_gender CHAR(1), 
    inp_dob DATE
)
RETURNS INTEGER
LANGUAGE PLPGSQL
AS $$
    DECLARE
        passenger_id INTEGER;
    BEGIN
        INSERT INTO passenger(name, gender, dob) VALUES (inp_name, inp_gender, inp_dob);
        SELECT last_value INTO passenger_id FROM passenger_passenger_id_seq;
        RETURN passenger_id;
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
            ) ;
        ELSE
            avl_seat := (
                SELECT j.available_sl_berths
                FROM journey j
                WHERE j.train_id = inp_train_id AND j.doj = inp_doj
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
        INSERT INTO tickets (train_id, doj, number_of_passengers, coach_type) VALUES (inp_train_id, inp_doj, inp_num_passenger, inp_coach_type);
        SELECT last_value INTO generated_pnr FROM tickets_pnr_seq;
        
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
    END;
$$;

