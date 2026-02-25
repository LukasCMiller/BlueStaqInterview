 # BlueStaqInterview
# System overview

This system is a Notes Vault API designed for users to store, get and delete notes in a vault.
This app works using Java JDK REST endpoints to do all these things. The JDK has the needed tools to create endpoints to be accessed via REST.
In order to store the notes there is a postgresql server that will run in its own container to store all notes. Notes are made up of three elements:
ID which is represented by a UUID, Content which is represented by a String and createdAt which is represented
by an Instant. I chose these three as the way to run because UUID provides a good way to create unique
IDs that wont overlap since the ID is the primary key. Content is a string since it is just the core of a note.
Timestamp is an Instant since it can be made at any time. I also did all 3 of the bonus objectives so there are the 3 new endpoints
for update, search and filter. There is also a limit on the length of the content string that length being 1000. I also added in authorization
using an API key which the API key is set in the DockerFile to be easily updated and changed and the backend code just references it so no need to be updated everywhere.


# Tech choices

I chose to limit the amount of external dependencies as much as possible to create a lightweight
and independent application. The only external dependency I have is the postgresql jar so that I
could make the connection to the database. By doing this all my HTTP connections are setup from only using
parts of the JDK. This app is fairly minimal so I felt like this was a good option to not require dependency on
anything else. I chose to use an API key for authorization because it provides a simple way to verify the access of the user but it does
have limitations such as it being hardcoded and not allowing for unique access. For testing the approach I chose was to make two different tests.
NotesHandlerTest handles the business layer by creating a mock database to use. I went in depth on this test since it requires less resources to run. 
I tested the error cases in this one rather than the other due to this resource. The ApiLevelTest runs with the actual app and database. This allows some
testing of the sql queries. I did not test all error cases in this test since it is more resource intensive and I believe that you should test the happy path
but not all paths when you have to use running apps.


# How to run the project and tests
To run just run the run.sh script in the top level of the repo. You will need docker
installed.

To run the business level tests run runBLTests.sh

To run the API level tests run runAPITests.sh. NOTE for API tests to work you will need the app running

# API usage examples

The way I tested it was via curl. I felt like this was a solid way to verify the endpoints
worked in the correct fashion since I can directly use them. 
Here are examples of each of the different endpoints: 

Create a note

curl -X POST -H "X-API-Key: super-secret-key" -d "id=c337dd2a-5f58-49c3-b282-d0c476ad5bdf&content=I am a note&timestamp=2026-02-23T01:25:19.013092Z" localhost:8080/notes

Get all notes

curl -H "X-API-Key: super-secret-key" localhost:8080/notes

Get a specific note

curl -H "X-API-Key: super-secret-key" localhost:8080/notes/c337dd2a-5f58-49c3-b282-d0c476ad5bdf

Update a note

curl -X PUT -H "X-API-Key: super-secret-key" -d "content=I am an updated note" localhost:8080/notes/c337dd2a-5f58-49c3-b282-d0c476ad5bdf

Search notes by keyword

curl -H "X-API-Key: super-secret-key" "localhost:8080/notes?search=updated"

Filter notes by date range

curl -H "X-API-Key: super-secret-key" "localhost:8080/notes?from=2026-01-01T00:00:00Z&to=2026-12-31T00:00:00Z"

Delete a note

curl -X DELETE -H "X-API-Key: super-secret-key" localhost:8080/notes/c337dd2a-5f58-49c3-b282-d0c476ad5bdf

And to check some error cases:

Missing API key - expect 401

curl localhost:8080/notes

Wrong API key - expect 401

curl -H "X-API-Key: wrongkey" localhost:8080/notes

Get note with invalid UUID - expect 400

curl -H "X-API-Key: super-secret-key" localhost:8080/notes/IAMNOTAUUID

Create note with invalid UUID - expect 400

curl -X POST -H "X-API-Key: super-secret-key" -d "id=IAMNOTAUUID&content=I am a note&timestamp=2026-02-23T01:25:19.013092Z" localhost:8080/notes

Create note with invalid timestamp - expect 400

curl -X POST -H "X-API-Key: super-secret-key" -d "id=c337dd2a-5f58-49c3-b282-d0c476ad5bdf&content=I am a note&timestamp=IAMNOTATIMESTAMP" localhost:8080/notes

  
Create note with content too long - expect 400 NOTE: This command requires Python3

curl -X POST -H "X-API-Key: super-secret-key" -d "id=c337dd2a-5f58-49c3-b282-d0c476ad5bdf&content=$(python3 -c 'print("a"*1001)')&timestamp=2026-02-23T01:25:19.013092Z" localhost:8080/notes

Update note with content too long - expect 400 NOTE: This command required Python3

curl -X PUT -H "X-API-Key: super-secret-key" -d "content=$(python3 -c 'print("a"*1001)')" localhost:8080/notes/c337dd2a-5f58-49c3-b282-d0c476ad5bdf

Update note with invalid UUID - expect 400

curl -X PUT -H "X-API-Key: super-secret-key" -d "content=updated content" localhost:8080/notes/IAMNOTAUUID

Filter with invalid timestamp - expect 400

curl -H "X-API-Key: super-secret-key" "localhost:8080/notes?from=IAMNOTATIMESTAMP"

Delete note with invalid UUID - expect 400

curl -X DELETE -H "X-API-Key: super-secret-key" localhost:8080/notes/IAMNOTAUUID


# Assumptions, tradeoffs, and future improvements

Assumptions: This is assuming you have docker installed to just run the app and need some way to run shell scripts (I use gitbash on windows). 
However, if you want to run the tests you will also need  Java installed.I also assumed that this app would not scale
beyond this. If this was a full app that a customer wanted to be serviced for years I wouldve used something
like SpringBoot so that we could more easily scale it up. 

Tradeoffs: This app is not highly scalable. Since I chose not to use a framework to be lightweight but if in the future this 
app needed to grow it would run into issues running. I put the database on a separate container so that
if the app crashes for some reason the database will stay up however this takes more resources than just
running them together in one container. The reason I chose a postgresql database was because in memory felt too
limited. You would only ever be able to have X number of notes but there would be some advantages such as being
able to access notes quickly. I didn't use a noSQL database mainly due to lack of familiarity with it. However, with the configuration you 
can only have one connection meaning there could be bottlenecks or errors with multiple users. A possible fix for this issue would be to use HikariCP to create a connection pool.
For the API key if I were to use something like Spring I would opt to use a JWT since Spring has easy ways to handle JWT and would allow for more secuirty.

Future improvements: Adding both in app memory along with a database with a LRU algorithm after X number of notes. Say
after 100 notes are stored we would remove the LRU. All notes would be stored in the database as a source of truth but the in memory would allow common and recently created notes to be quick to access.
I would also add another POST endpoint to the app where the user only provides content. With the approach I used it would be easy for the backend
to generate a random UUID and Instant.now to store the note then return the UUID in the return message.
