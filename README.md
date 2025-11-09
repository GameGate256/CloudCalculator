# Cloud Calculator

Java socket-based arithmetic calculator  

---

## Overview

Client and server communicate through a simple text protocol over TCP  
The client sends an arithmetic expression  
The server parses the request, performs the calculation, and returns a single-line response  

Supported operations:

```
ADD a b
SUB a b
MUL a b
DIV a b
PING
QUIT
```

---

## Project Structure

```
src/
 ├─ Server.java     // Multi-threaded TCP server using thread pool
 ├─ Client.java     // Console client for manual commands
 ├─ Config.java     // Handles server_info.dat configuration file
 └─ Main.java       // Combined test runner for local verification
server_info.dat     // Default host and port configuration
```

---

## Protocol Definition

### Request  
Plain ASCII command line

```
ADD 10 20
```

### Response  

```
RESP 200 OK result=30
RESP 400 ERROR type=DIV_BY_ZERO message=divided by zero
```

### Status and Error Types  

| Type | Description |
|------|--------------|
| INVALID_OP | Unsupported operation |
| ARITY_ERROR | Invalid number of arguments |
| FORMAT_ERROR | Invalid numeric format |
| DIV_BY_ZERO | Division by zero |

---

## Build and Run

Compile  

```
javac Server.java Client.java Config.java Main.java
```

Run server  

```
java Server
```

Run client  

```
java Client
```

Run integrated smoke test  

```
java Main
```

---

## Example Session

Client input  

```
> ADD 10 20
> DIV 25 0
> PING
> QUIT
```

Server output  

```
RESP 200 OK result=30
RESP 400 ERROR type=DIV_BY_ZERO message=divided by zero
RESP 204 NO_CONTENT
RESP 200 OK result=bye
```

---

## Author

Junseo Heo 
