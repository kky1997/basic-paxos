import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessPool //NOTE TO SELF, MUST ALWAYS SET PROPOSE TIMEOUTS IN PROCESSPOOL METHODS WHICH REQUIRE AN ACCEPTOR OR PROPOSER TO DIE
{
    //public static int numberOfThreads = 9;
    volatile public static int numberOfProposers = 0;
    public static AtomicInteger numberOfAcceptors = new AtomicInteger(0); //this one changes
    public static int NUMBER_OF_ACCEPTORS = 0; //this one does not change

    public static AtomicInteger leaderCount = new AtomicInteger(0);

    public static final int TIME_OUT_CONSTANT = 5000; //5000ms base timeout for propose
    public static final int DEFAULT_TIME_OUT_CONSTANT_PREPARE_SERVERSOCKET = 10000;
    public static final int DEFAULT_TIME_OUT_CONSTANT_PREPARE_SOCKET = 6000;

    //public static int selection = 0;
    //public static int round = 1;

    public void TestCase_1() throws IOException, InterruptedException
    {
        numberOfProposers = 1;
        numberOfAcceptors.set(8);
        NUMBER_OF_ACCEPTORS = 8;
        Thread learner = new learner("learner", "M1 is president with 5 votes");
        proposer proposer1 = new proposer("M1");
        for(int i = 1; i < NUMBER_OF_ACCEPTORS + 1; i++)
        {
            acceptor acceptor = new acceptor("M" + Integer.toString(i + 1));
            acceptor.start();
            
        }
        learner.start();
        proposer1.prepare(0);
                
        if(proposer1.leader == true)
        {
            proposer1.propose(0);
        }
    }

    public void TestCase_2() throws IOException, InterruptedException
    {
        numberOfProposers = 2;
        numberOfAcceptors.set(7);
        NUMBER_OF_ACCEPTORS = 7;
        int Majority = NUMBER_OF_ACCEPTORS/2 + 1;
        Thread learner = new learner("learner", "M2 is president with 4 votes");
        proposer proposer1 = new proposer("M1");
        proposer proposer2 = new proposer("M2");
        for(int i = 2; i < NUMBER_OF_ACCEPTORS + 2; i++)
        {
            acceptor acceptor = new acceptor("M" + Integer.toString(i + 1));
            acceptor.start();
        }
        learner.start();
        proposer2.prepare(0);
        proposer1.prepare(0);
        
        if(proposer1.leader == true && proposer1.prepareFlag == true && proposer2.prepareFlag == true)
        {
            proposer1.propose(0);
        }
        //TimeUnit.SECONDS.sleep(1);
        if(proposer2.leader == true && proposer1.prepareFlag == true && proposer2.prepareFlag == true)
        {
            proposer2.propose(0);
        }
    }

    public void TestCase_3() throws IOException, InterruptedException
    {
        numberOfProposers = 3;
        numberOfAcceptors.set(6);
        NUMBER_OF_ACCEPTORS = 6;
        Thread learner = new learner("learner", "M1 is president with 4 votes");
        proposer proposer1 = new proposer("M1");
        proposer proposer2 = new proposer("M2");
        proposer proposer3 = new proposer("M3", 1);

        for(int i = 3; i < NUMBER_OF_ACCEPTORS + 3; i++)
        {
            acceptor acceptor = new acceptor("M" + Integer.toString(i + 1));
            acceptor.start();
        }
        learner.start();
        proposer1.prepare(0);
        proposer3.prepare(0);
        proposer2.prepare(0);
        if(proposer1.leader == true)
        {
            proposer1.propose(0);
        }
        if(proposer2.leader == true)
        {
            proposer2.propose(0);
        }
        if(proposer3.leader == true)
        {
            proposer3.propose(0);
        }
    }

    //M3 always in woods, M2 fails for 2 rounds, on 3 round, they don't fail
    //M2 is never working
    public void Test_Case4(int round) throws IOException
    {
        numberOfProposers = 1;
        numberOfAcceptors.set(8);
        NUMBER_OF_ACCEPTORS = 8;
        Driver.threadCount.set(NUMBER_OF_ACCEPTORS);
        System.out.println("Round: " + round + "\n");
        Random random = new Random();
        int roundCounter = round;
        int randomResponseTime = 0;
        int localTimeOut = TIME_OUT_CONSTANT; //5000
        Thread learner = new learner("learner", "M1 is president with 5 votes");
        proposer proposer1 = new proposer("M1");
        
        for(int i = 1; i < NUMBER_OF_ACCEPTORS + 1; i++)
        {
            acceptor acceptor = new acceptor("M" + Integer.toString(i + 1));
            if(acceptor.get_Name().equalsIgnoreCase("M2"))
            {
                acceptor.set_ResponseTime(3);
                acceptor.proposeSocketTimeOut = localTimeOut;
                if(roundCounter < 3)
                {
                    acceptor.caseSelectorM2 = 1;
                }
                else
                {
                    acceptor.caseSelectorM2 = 2;
                }
            }
            else if(acceptor.get_Name().equalsIgnoreCase("M3"))
            {
                acceptor.set_ResponseTime(2);
                acceptor.proposeSocketTimeOut = localTimeOut;
                acceptor.caseSelectorM3 = 1; //always in woods
                
            }
            else
            {
                randomResponseTime = random.nextInt(3)+1;
                acceptor.set_ResponseTime(randomResponseTime);
                acceptor.proposeSocketTimeOut = localTimeOut;
            }
            acceptor.start();
        }
        learner.start();

        proposer1.prepare(0);
                
        if(proposer1.leader == true)
        {
            proposer1.propose(0);
        }
    }

    //m3 never in woods, M2 acceptor fails for 2 rounds, on 3 round, they don't fail
    //m2 is also never working
    public void Test_Case5(int round) throws IOException 
    {
        numberOfProposers = 1;
        numberOfAcceptors.set(8);
        NUMBER_OF_ACCEPTORS = 8;
        Driver.threadCount.set(NUMBER_OF_ACCEPTORS);
        System.out.println("Round: " + round + "\n");
        Random random = new Random();
        int roundCounter = round;
        int randomResponseTime = 0;
        int localTimeOut = TIME_OUT_CONSTANT +  500; //4500
        Thread learner = new learner("learner", "M1 is president with 5 votes");
        proposer proposer1 = new proposer("M1");
        
        for(int i = 1; i < NUMBER_OF_ACCEPTORS + 1; i++)
        {
            acceptor acceptor = new acceptor("M" + Integer.toString(i + 1));
            if(acceptor.get_Name().equalsIgnoreCase("M2"))
            {
                acceptor.set_ResponseTime(3);//isM2Working);
                acceptor.proposeSocketTimeOut = localTimeOut;
                if(roundCounter < 3)
                {
                    acceptor.caseSelectorM2 = 1;
                }
                else
                {
                    acceptor.caseSelectorM2 = 2;
                }
            }
            else if(acceptor.get_Name().equalsIgnoreCase("M3"))
            {
                acceptor.set_ResponseTime(2);
                acceptor.proposeSocketTimeOut = localTimeOut;
                acceptor.caseSelectorM3 = 2; //always NOT in the woods
                
            }
            else
            {
                randomResponseTime = random.nextInt(3)+1;
                acceptor.set_ResponseTime(randomResponseTime);
                acceptor.proposeSocketTimeOut = localTimeOut;
            }
            acceptor.start();
        }
        learner.start();

        proposer1.prepare(0);
                
        if(proposer1.leader == true)
        {
            proposer1.propose(0);
        }
    }

    //m2 is never working and M2 as proposer will fail 2 rounds after prepare
    //then m2 as proposer will not fail round 3
    //m2 is the highest ID proposer in this test case, so no consensus until M2 doesn't fail in round 3
    //m3 as acceptor will be randomly in the woods or not
    public void Test_Case6(int round) throws InterruptedException, IOException
    {
        numberOfProposers = 2;
        numberOfAcceptors.set(7);
        NUMBER_OF_ACCEPTORS = 7;
        Driver.threadCount.set(NUMBER_OF_ACCEPTORS);
        System.out.println("Round: " + round + "\n");
        Random random = new Random();
        int roundCounter = round;
        int randomResponseTime = 0;
        int localTimeOut = TIME_OUT_CONSTANT + 2500;
        Thread learner = new learner("learner", "M1 is president with 4 votes");
        proposer proposer1 = new proposer("M1");
        proposer proposer2 = new proposer("M2", 3);
        if(roundCounter < 3)
        {
            proposer2.proposeOrNot = 1;
        }
        else
        {
           proposer2.proposeOrNot = 2;
        }
        
        for(int i = 2; i < NUMBER_OF_ACCEPTORS + 2; i++)
        {
            acceptor acceptor = new acceptor("M" + Integer.toString(i + 1));
            if(acceptor.get_Name().equalsIgnoreCase("M3"))
            {
                acceptor.set_ResponseTime(1);
                acceptor.proposeSocketTimeOut  = localTimeOut;
            }
            else
            {
                randomResponseTime = random.nextInt(3)+1;
                acceptor.set_ResponseTime(randomResponseTime);
                acceptor.proposeSocketTimeOut  = localTimeOut;
            }
            acceptor.start();
        }
        learner.start();
        proposer1.prepare(0);
        proposer2.prepare(0);
                  
        if(proposer1.leader == true)
        {
            proposer1.propose(0);
        }
        if(proposer2.leader == true)
        {
            proposer2.propose(0);
        }
    }

    
    

    //this test is with two proposers and is the only truly randomised one
    //this test will randomly decide if M2 is working, if they are, they respond instantly and they will never fail after sending a prepare as proposer
    //this test will also randomly decide if M3 is in the woods or not.
    public void Test_Case7(int round) throws InterruptedException, IOException
    {
        numberOfProposers = 2;
        numberOfAcceptors.set(7);
        NUMBER_OF_ACCEPTORS = 7;
        Driver.threadCount.set(NUMBER_OF_ACCEPTORS);
        System.out.println("Round: " + round + "\n");
        System.out.println("Seeing if M2 is at work");
        Random random = new Random();
        int atWork = 1;
        int notAtWork = 3;
        int randomResponseTime = 0;
        int isM2Working = random.nextBoolean() ?  atWork : notAtWork; //generate randomly whether M2 is at work or not, if true = working, if false = not working
        int localTimeOut = TIME_OUT_CONSTANT + 2500;
        if(isM2Working == 1)
        System.out.println("M2 is working at the cafe");
        else
        System.out.println("M2 is not working at the cafe");
        Thread learner = new learner("learner", "M1 is president with 4 votes");
        proposer proposer1 = new proposer("M1");
        proposer proposer2 = new proposer("M2", isM2Working);
        
        for(int i = 2; i < NUMBER_OF_ACCEPTORS + 2; i++)
        {
            acceptor acceptor = new acceptor("M" + Integer.toString(i + 1));
            if(acceptor.get_Name().equalsIgnoreCase("M3"))
            {
                acceptor.set_ResponseTime(2);
                acceptor.proposeSocketTimeOut  = localTimeOut;
            }
            else
            {
                randomResponseTime = random.nextInt(3)+1;
                acceptor.set_ResponseTime(randomResponseTime);
                acceptor.proposeSocketTimeOut  = localTimeOut;
            }
            acceptor.start();
        }
        learner.start();
        proposer1.prepare(0);
        proposer2.prepare(0);
                  
        if(proposer1.leader == true)
        {
            proposer1.propose(0);
        }
        if(proposer2.leader == true)
        {
            proposer2.propose(0);
        }
    }

    //3 proposers, M2, has a chance to fail or not, regardless M3 will always succeed and carry the consensus
    //DEPRECATED NOT USING
    public void Test_Case8(int round) throws IOException
    {
        numberOfProposers = 3;
        numberOfAcceptors.set(6);
        NUMBER_OF_ACCEPTORS = 6;
        Driver.threadCount.set(NUMBER_OF_ACCEPTORS);
        System.out.println("Round: " + round + "\n");
        Random random = new Random();
        //int roundCounter = round;
        int atWork = 1;
        int notAtWork = 3;
        int randomResponseTime = 0;
        System.out.println("Seeing if M2 is at work (if 1 they are working, if 3 they are not)");
        int isM2Working = random.nextBoolean() ?  atWork : notAtWork; //generate randomly whether M2 is at work or not, if true = working, if false = not working
        int localTimeOut = TIME_OUT_CONSTANT + 3500;
        if(isM2Working == 1)
        System.out.println("M2 is working at the cafe");
        else
        System.out.println("M2 is not working at the cafe");
        Thread learner = new learner("learner");
        proposer proposer1 = new proposer("M1");
        proposer proposer2 = new proposer("M2", 3);//isM2Working);
        proposer proposer3 = new proposer("M3", 1);
        learner.start();
        for(int i = 3; i < NUMBER_OF_ACCEPTORS + 3; i++)
        {
            randomResponseTime = random.nextInt(3)+1;
            acceptor acceptor = new acceptor("M" + Integer.toString(i + 1));
            acceptor.set_ResponseTime(randomResponseTime);
            acceptor.proposeSocketTimeOut = localTimeOut; 
            acceptor.start();
        }
        
        proposer1.prepare(0);
        proposer2.prepare(0);
        proposer3.prepare(0);
        
        if(proposer1.leader == true)
        {
            proposer1.propose(0);
        }

        if(proposer2.leader == true)
        {
            proposer2.propose(0);
        }

        if(proposer3.leader == true)
        {
            proposer3.propose(0);
        }

    }

    //p2 prepares before p1
    public void Test_Case9(int round) throws InterruptedException, IOException
    {
        numberOfProposers = 2;
        numberOfAcceptors.set(7);
        NUMBER_OF_ACCEPTORS = 7;
        Driver.threadCount.set(NUMBER_OF_ACCEPTORS);
        System.out.println("Round: " + round + "\n");
        Random random = new Random();
        int roundCounter = round;
        int randomResponseTime = 0;
        //int localPrepareTimeOutServerSocket = DEFAULT_TIME_OUT_CONSTANT_PREPARE_SERVERSOCKET - 6000;
        //int localPrepareTimeOutSocket = DEFAULT_TIME_OUT_CONSTANT_PREPARE_SOCKET - 2000;
        int localProposeTimeOut = TIME_OUT_CONSTANT;
        Thread learner = new learner("learner", "M2 is president with 4 votes");
        proposer proposer1 = new proposer("M1");
        proposer proposer2 = new proposer("M2", 3);
        if(roundCounter < 3)
        {
            proposer2.proposeOrNot = 1;
        }
        else
        {
           proposer2.proposeOrNot = 2;
        }
        
        for(int i = 2; i < NUMBER_OF_ACCEPTORS + 2; i++)
        {
            acceptor acceptor = new acceptor("M" + Integer.toString(i + 1));
            if(acceptor.get_Name().equalsIgnoreCase("M3"))
            {
                acceptor.set_ResponseTime(1);
                acceptor.proposeSocketTimeOut  = localProposeTimeOut;
                //acceptor.prepareSocketTimeOutServerSocket = localPrepareTimeOutServerSocket;
                //acceptor.prepareSocketTimeOutSocket = localPrepareTimeOutSocket;
            }
            else
            {
                randomResponseTime = random.nextInt(3)+1;
                acceptor.set_ResponseTime(randomResponseTime);
                acceptor.proposeSocketTimeOut  = localProposeTimeOut;
                //acceptor.prepareSocketTimeOutServerSocket = localPrepareTimeOutServerSocket;
                //acceptor.prepareSocketTimeOutSocket = localPrepareTimeOutSocket;
            }
            acceptor.start();
        }
        learner.start();
        proposer2.prepare(0);
        proposer1.prepare(0);
                  
        if(proposer1.leader == true)
        {
            proposer1.propose(0);
        }
        if(proposer2.leader == true)
        {
            proposer2.propose(0);
        }
    }
    
}