 # BlueStaqInterview
· System overview
This system is a Notes Vault API designed for users to store, get and delete notes in a vault.
This app works using Java JDK REST endpoints to do all these things. In order to store the notes there
is a postgresql server that will run in its own container to store all notes. Notes are made up of three elements:
ID which is represented by a UUID, Content which is represented by a String and createdAt which is represented
by an Instant. I chose these three as the way to run because UUID provides a good way to create unique
IDs that wont overlap since the ID is the primary key. Content is a string since it is just the core of a note.
Timestamp is an Instant since it can be made at any time.

· Tech choices
I chose to limit the amount of external dependencies as much as possible to create a lightweight
and independent application. The only external dependency I have is the postgresql jar so that I
could make the connection to the database. By doing this all my HTTP connections are setup from only using
parts of the JDK. This app is fairly minimal so I felt like this was a good option to not require dependency on
anything else.

· How to run the project and tests
To run just run the run.sh script in the top level of the repo. You will need docker
installed.
To run the business level tests run runBLTests.sh
To run the API level tests run runAPITests.sh. NOTE for API tests to work you will need the app running

· API usage examples
The way I tested it was via curl. I felt like this was a solid way to verify the endpoints
worked in the correct fashion. Here are examples of each of the different endpoints: 
curl -d "id=c337dd2a-5f58-49c3-b282-d0c476ad5bdf&content=I am a note&timestamp=2025-12-27T04:50:49.438530490Z" localhost:8080/notes
to insert. curl localhost:8080/notes to get all notes. curl localhost:8080/notes/c337dd2a-5f58-49c3-b282-d0c476ad5bdf. curl -X DELETE localhost:8080/notes/c337dd2a-5f58-49c3-b282-d0c476ad5bdf.
NOTE all the UUIDs need to match to handle all requests correctly.

· Assumptions, tradeoffs, and future improvements
Assumptions: This is assuming you have docker installed. I also assumed that this app would not scale
beyond this. If this was a full app that a customer wanted to be serviced for years I wouldve used something
like SpringBoot so that we could more easily scale it up. Tradeoffs: This app is not highly
scalable. Since I chose not to use a framework to be lightweight but if in the future this 
app needed to grow it would run into issues running. I put the database on a separate services so that
if the app crashes for some reason the database will stay up however this takes more resources than just
running them together in one pod. The reason I chose a postgresql database was because in memory felt too
limited. You would only ever be able to have X number of notes but there would be some advantages such as being
able to access notes quickly. I didnt use a noSQL database mainly due to lack of familiarity with it.
Future improvements: Adding both in app memory along with a database with a LRU algorithm after X number of notes. Say
after 100 notes are stored we would move the LRU note to the database to allow for recently used notes to be faster access.
