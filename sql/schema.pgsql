CREATE TABLE IF NOT EXISTS trains (
    train_id INTEGER PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS journey(
    train_id INTEGER NOT NULL,
    doj DATE NOT NULL,
    number_ac_coaches INTEGER NOT NULL,
    number_sl_coaches INTEGER NOT NULL,
    available_ac_berths INTEGER NOT NULL,
    available_sl_berths INTEGER NOT NULL,
    FOREIGN KEY (train_id) REFERENCES trains(train_id),
    PRIMARY KEY (train_id, doj)
);

CREATE TABLE IF NOT EXISTS tickets(
    train_id INTEGER NOT NULL,
    doj DATE NOT NULL,
    pnr SERIAL PRIMARY KEY,
    number_of_passengers INTEGER NOT NULL,
    coach_type CHAR(2) NOT NULL,
    FOREIGN KEY (train_id, doj)
        REFERENCES journey(train_id, doj)
);

CREATE TABLE IF NOT EXISTS passenger(
    passenger_id SERIAL PRIMARY KEY,
    name VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS booked(
    passenger_id INTEGER NOT NULL,
    pnr INTEGER NOT NULL,  
    berth_no INTEGER NOT NULL,
    coach_no INTEGER NOT NULL,
    FOREIGN KEY (pnr)
        REFERENCES tickets(pnr),
    FOREIGN KEY (passenger_id)
        REFERENCES passenger(passenger_id)
);

CREATE TABLE IF NOT EXISTS coach_info(
    item VARCHAR PRIMARY KEY,
    value INTEGER NOT NULL
);