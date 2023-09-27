# Railway Reservation System
A database project to book railway tickets with main focus on database design,concurrency and scalability.

It is made using Java and PostgreSQL.

It a client-server program where the clients send multiple requests simultaneously, and the server uses multithreading to use all available cores on system to address these requests efficiently.

Client threads read files from [Input folder](Input) and send data to the server and write response to respective files in [Output folder](Output).

Server create a new thread for every client thread. Each thread on server create its own connection to database, process the request, perform changes according to database and send back the response.

# ER Diagram
![ERD Diagram](https://github.com/navroop005/Railway-Reservation-System/assets/75077323/45f0e937-b8a5-43db-858e-f5381227cf0b)


# Compile and run
Create database named `railway_reservation_system` in postgres. (make changes to [open_database function in Server.java](src/Server.java#L29) if required)

Compile:
```
javac -cp ".:./lib/*" -d ./bin ./src/*
```

Run Server:
```
java -cp .:./bin:./lib/* Server
```
Run Client:
```
java -cp .:./bin:./lib/* client
```

# Contributors
Navroop Singh=2020CSB1101 <br>
Nishant Verma=2020CSB1103 <br>
Vinit Nana Hagone=20202CSB1361 <br>
