# Backend Test

Design and implement a RESTful API (including data model and the backing implementation) for money transfers between accounts.
#### Explicit requirements:
1. You can use Java or Kotlin.
2. Keep it simple and to the point (e.g. no need to implement any authentication).
3. Assume the API is invoked by multiple systems and services on behalf of end users.
4. You can use frameworks/libraries if you like (​except Spring​), but don't forget about
requirement #2 and keep it simple and avoid heavy frameworks.
5. The datastore should run in-memory for the sake of this test.
6. The final result should be executable as a standalone program (should not require a
pre-installed container/server).
7. Demonstrate with tests that the API works as expected.

#### Implicit requirements:
1. The code produced by you is expected to be of high quality.
2. There are no detailed requirements, use common sense.

#### Test
    ./gradlew clean test

#### Build
    ./gradlew clean build
#### Launch
    java -jar build/libs/revolut-1.0-SNAPSHOT.jar
It starts listening to requests at http://0.0.0.0:8080

#### Main API
    curl --location --request POST 'http://0.0.0.0:8080/accounts/1/transfer/2' \
    --header 'Content-Type: application/json' \
    --header 'Idempotency-Key: d697a9e6-d312-4d45-8eea-aec7f9eb8f0c' \
    --data-raw '{
    	"amount":1.0
    }'

NB: every request to the server must contain Idempotency-Key in the headers so that client could retry the request in case of network issues.

#### Project structure
    src/main/resources/db/migration/V1.0.0__Base_version.sql - database structure
    src/main/resources/db/migration/V1.0.1__Add_consistency_checks.sql - data consistency checks
    src/main/kotlin/com/revolut/business/TransferRepository.kt - create transfer in database
    src/test/groovy/com/revolut/tests/TransfersTests.groovy - tests