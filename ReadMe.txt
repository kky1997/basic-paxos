Overview:

    All code written and compiled in java 8.

    I have attempted a decentralized design, where Proposers and Acceptors contact each other directly. Proposers and Acceptors are seperate entities in my implementation. This means that in a council of
    9 members, if M1 is a Proposer, the other 8 members are Acceptors (they are distinctly different though they inherit from the same parent class "Member"). Additionally, I have a single seperate entity,
    called Learner, that exists outside the 9 members, and is just there to recieve Votes/Accepted Messages from Acceptors. This is so that if a Proposer were to die after sending all its "Propose Messages",
    there is still a singleton process alive to recieve votes and announce the outcome. This implementation does NOT support "n" members.

    My Paxos also continues till agreement is reached, incrementing ID each round. ID's are based on their member names, eg. M1 will start with ID 0.1, M2 will start with ID 0.2, etc. Each subsequent round adds
    +1 to their ID; so in "round 2" M1's ID will be 1.1, M2's ID will be 1.2, etc.

    ProcessPool.java is the class used to house all the profiles/tests and is where all the spawning of threads is handled.

    at Phase 1: Proposer/Proposers will spawn Prepare Threads equal to the number of acceptors, each thread handling the sending of a prepare(ID) message as well as the response (Promise). As soon as a quorum (Majority + 1) 
    of these Prepare Threads have recieved a promise, the proposer will begin to Phase 2.

    at Phase 2: Proposer/Proposers will spawn a number of Propose Threads equal to the number of Promises it recieved. These Propose Threads will contact each of the Acceptors whom were the fastest to send back "Promise Messages" in Phase 1.
                If all the Acceptors are alive (in presence of no failures), then they will each send back and "Accepted Message" to both the original Proposer they promised and also a Learner (a seperate entity existing outside the 9 council members).
                If the Learner has recieved Majority+1 "Accepted Messages", then it will announce the result of the vote.


    This Paxos implementation also supports response times based on the profiles of members provided in the spec:

    M2(As Acceptor): "Sometimes doesn't reply to all messages", to simualte this, M2 as an Acceptor will sometimes not reply to Propose() messages even if it had sent a promise in the previous phase.
                     This will cause the Learner to be short 1 vote (as it's expecting votes from all those who promised quickest to make up the quorum). Hence, the Learner will eventually timeout, 
                     and another round will start. If this occurs, a message will be printed "M2 randomly decided not to reply to propose message", else another message will be printed "M2 is just replying slowly",
                     signifying that M2 will still reply, but at a "late" response time.
                     There is a 1 in 3 chance for M2 to "decided not to reply to propose message". However, in the tests provided, this is scripted to only happen for the first two rounds.

    M2(As Proposer): "Sometimes doesn't reply to all messages", to simulate this, M2 as Proposer has a 1 in 3 chance to not send a "Propose Message" even after it has sent "Prepare Messages" and recieved
                     enough promises back. If this happens a message will be printed "proposer going offline without proposing". If M2 is the highest ID proposer, then this will cause Paxos to be unable to
                     reach consensus. Hence, the Learner will timeout and another round will start. In 2 Proposer tests, this is scripted to only happen for the first two rounds.

    M3(As Acceptor): "Sometimes emails completely do not get to M3", to simulate this, there is a 1 in 3 chance for M3 to not reply to either "Prepare or Propose Messages". If this is the case,
                     "M3 in woods today" will be printed. Otherwise "M3 not in the woods today" will be printed, and instead M3 will just have a "medium" response time.

    M3(As Proposer): "Sometimes emails completely do not get to M3", to simulate this, the same mechanism is used as M2(As Proposer). The only difference would be in the name of the Propser,
                     M3 instead of M2.

    M4-M9(Acceptors): Have random response times. This is generated through random.nextInt(). They will either have a response time of; "1 - instant", "2 - medium (1000ms)", or "3 - late (2000ms)".
    
    
    To try and minimise as much randomness and force some sense of determinism, I have tried to designed my PAXOS to allow the control of which proposer will prepare/propose first. This means that depending on
    the order that the Proposers are started (even though they are started immediately one after another), the result of the consensus will be different, but will be deterministic. This should show still
    that the logic of this algorithm works as defined by the paxos protocol, but allow users to see what happens if a smaller ID starts first or if a larger ID starts first.

    Example: In a 2 proposer situation, if M1 is started then M2, M1 will always Prepare/Propose and have it's value stored by Acceptors. However, M2 (higher ID), will always be the Proposer to bring
             the consensus to an end. This means that M2 will be the one to send the final "Propose Messages" with M1's value (it's ID which is 1). Hence agreement will be reached on M1 being president,
             but M2 would have been the Proposer to have pushed this value to agreement.

             Consequently, If M2 is started then M1, M2 will Prepare/Propose, while M1 gets "FAILED Messages" back from Acceptors (as it's ID is less than the currently accepted ID). Hence, M2 will have 
             it's value push to agreement, by itself. So Agreement will be reached on M2 being president, with M2 being the Proposer that pushed this value to agreement.
    
    IMPORTANT NOTE:
    A bit of a limtation is that I have given a very generous timeout to the Learner (35s), this is to ensure as many sockets are able to close as possible. A new round will only start once the Learner timesout,
    so the time between each round to start may sometimes feel like it takes quite awhile, however it is just this timeout. Please be mindful of this limitation and I apologise for some of the tests 
    requiring a lot of waiting time. 


How to run:

    To run this Paxos implementation, firstly "javac *.java" to compile all files. Then simply run the "Driver" via "java Driver" terminal command. This will execute the Driver which will present a menu
    which will describe each of the run cases/tests/profiles. Then just type in the number of corresponding to the desired run case/test/profiles and press Enter.

    Eg. if one wanted to run the menu option of "2 - Basic paxos with 2 proposers proposing simultaneously and immediate response times", they would simply type "2" and press enter.

    The test will be over when "[TEST SUCCESSFUL - CONSENSUS REACHED]" is printed. This may take a few rounds when using the variable response time profiles.


Testing:

    The Driver will list all the run time scenarios which I have written to try and cover all scenariors in the spec. When a tester runs any one of the profiles listed in Driver, the program will start,
    and it will run until a consensus it reached, in which the program will print the result and terminate. Note that it may take multiple rounds for a consensus to be reached for some of the test cases.

    Since the result for each case is deterministic, I supply the expected result at the end of the Paxos protocol when the result of the protocol is reached. The result should always match the provided
    expected result.

    I have tried to control how many rounds each test will take:
    Eg. if the highest ID proposer fails after Prepare for the first 2 rounds, then on the 3rd round they do not fail, reaching agreement should take 3 rounds
    or
    Eg. if an Acceptor that sent a promise fails to send an Accepted message for the first 2 rounds, then on the 3rd round they do not fail, reaching agreement shoudl take 3 rounds
    
    however, with tests 6 and 7 (both are 2 Proposer tests), unfortunately there are in some cases some random thread behaviour that I have not been successful in trying to control, which may cause
    these tests to take more than 3 rounds. Though unintentional, these cases of random thread behaviour would likely model some further Acceptor failures beyond what the spec asks for.
    The important outcome however, is that through all my test runs, the protocol does always reach agreement on the correct value carried by the correct Proposer, it is just the number of rounds
    may be different between each test run of a few of the tests.

    Tests 1 - 3 are simple 1, 2, and 3 proposers and everyone has instant response times. In test 3, The order of Proposers is M1, M3, then M2. So M1's value will be carried to agreement, but
    by M3 (highest ID). M2 will just recieve "FAILED Messages" as it's ID is less than M3's. 
    NOTE: from my tests over a few different systems, only when using the uni system through SSH, there is a very small chance that tests 2 and 3 livelock if performed quickly one after another.
          Unfortunately, i've been unsuccessful in determining exactly why, however most of the time they should function as intended. My guess is either a socket connection problem or a thread 
          scheduling problem.

    Tests 4 & 5 are with a single Proposer, both tests will have M2(Acceptor), fail to respond to "Propose Message" with and "Accepted Message", despite previously sending "Promise Messages". This will
    only happen for the first two rounds, and at round 3 onwards, agreement will be reached (99% of the time at round 3 agreement is reached). The only difference between these two tests is whether M3 (Acceptor)
    is in the woods or not, which influences which whether M3 sends back a promise/accepted or not. M4 - M9(Acceptors) will have random response times between immediate, medium, and late.

    Tests 6 & 7 are with two Proposers, both tests will have M2(Proposer), fail after sending "Prepare Messages" right before M2 is about to send "Propose Messages". Since M2 is the higher ID Proposer
    the protocol will fail to reach agreement. This will only happen for the first two rounds, and at round 3 onwards, agreement will be reached. The difference between test 6 & 7 is just the order in which
    M1 and M2 Proposers start.
    test 6: M1 -> M2 (so M1 value used, but M2 brings it to agreement)
    test 7: M2 -> M1 (so M2 value used and M2 brings it to agreement)
    Since M2(Proposer) only fails the first two rounds, this is akin to them "Not working at the cafe" for the first two rounds, and then on the third round "They are working at the cafe".

    Test 8 is almost completely random, however, M2(Proposer) is always working, so they function the exact same as M1(Proposer). This means they will never not send "Propose Messages".

    Test 9 is a truly random test with two Proposers, M3(Acceptor) may be in woods or not, M2(Proposer) may be working or not working (if they are not working they have a chance to never send "Propose Messages").

    Test 10 uses M1 and M3 as proposers now. Very similar to test 6, where M3(proposer) will fail at sending "Prepare Messages" right before it is about to send "Propose Messages. This will happen for rounds 1 & 2.
    At round 3, M2(Acceptor) will fail to respond to Propose with an "Accepted Message". Hence, this test should resolve at round 4 onwards.

    
Notable Print Outs:

    "GENERATING RANDOM NUMBER AND SEEING IF M3 is in woods" followed by either "M3 is not in the woods today" or "M3 is in woods today" --- signify status of M3(Acceptor)

    "live or die?" followed by either "M2 proposer not going offline" or "M2 proposer going offline without proposing" ---- signify status of M2(Proposer)

    "GENERATING RANDOM NUMBER AND SEEING IF M2 WILL REPLY (DODGY INTERNET)" followed by either "M2 is just replying slowly" or "M2 randomly decided not to reply to propose message " ---- signify status of M2(Acceptor)

    "Mx is PROPOSING TO port 800x" --- signifies which Proposer is proposing to which Acceptors ports (Ports are based on the number in their name + 8000, eg. M3 is 3 + 8000 = 8003)

    "SENT BY proposerID: x" --- signifies which proposer's ID was sent with the "Accepted Message", it will always be the highest ID proposer.
    
    following is only applicable for test 8 & 9:

        "Seeing if M2 is at work" followed by either "M2 is not working at the cafe" or "M2 is working at cafe --- signify whether M2(Proposer) is working or not
                        

