import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Driver 
{
    public static boolean nextRound = false;
    public int responseTime = 0;
    public int numberOfCouncilors = 0;
    public static int roundCount = 1;
    public static AtomicInteger threadCount = new AtomicInteger(0);

    //method to handle switch statement which will run the correct profile based on what selected profile the user makes.
    public static void ProcessInput(int profile) throws ClassNotFoundException, InterruptedException, IOException
    {
        switch(profile)
        {
            case 1:
            {
                ProcessPool pp = new ProcessPool();
                do
                {   
                    while(threadCount.get() !=0){};
                    pp.TestCase_1(roundCount);
                    while(learner.isAlive)
                    {
                        TimeUnit.SECONDS.sleep(3);
                    }
                    learner.isAlive = true;
                    roundCount++;
                    ProcessPool.leaderCount.set(0); //reset leadercount so that it doesn't have any previous counts from last round
                    ProcessPool.numberOfAcceptors.set(8); 
                    ProcessPool.numberOfProposers = 1;
                    AddressBook.AddressBookLookUp.clear();
                    proposer.ID_Modifier++;
                    proposer.globalAcceptorCounter.set(0); 
                }while(nextRound);
                break;
            }
            
            case 2:
            {
                ProcessPool pp = new ProcessPool();
                do
                {   
                    while(threadCount.get() !=0){};
                    pp.TestCase_2(roundCount);
                    while(learner.isAlive)
                    {
                        TimeUnit.SECONDS.sleep(3);
                    }
                    learner.isAlive = true;
                    roundCount++;
                    ProcessPool.leaderCount.set(0); //reset leadercount so that it doesn't have any previous counts from last round
                    ProcessPool.numberOfAcceptors.set(7); 
                    ProcessPool.numberOfProposers = 2;
                    AddressBook.AddressBookLookUp.clear();
                    proposer.ID_Modifier++;
                    proposer.globalAcceptorCounter.set(0); 
                }while(nextRound);
                break;
            }
            case 3:
            {
                ProcessPool pp = new ProcessPool();
                do
                {   
                    while(threadCount.get() !=0){};
                    pp.TestCase_3(roundCount);
                    while(learner.isAlive)
                    {
                        TimeUnit.SECONDS.sleep(3);
                    }
                    learner.isAlive = true;
                    roundCount++;
                    ProcessPool.leaderCount.set(0); //reset leadercount so that it doesn't have any previous counts from last round
                    ProcessPool.numberOfAcceptors.set(6); 
                    ProcessPool.numberOfProposers = 3;
                    AddressBook.AddressBookLookUp.clear();
                    proposer.ID_Modifier++;
                    proposer.globalAcceptorCounter.set(0); 
                }while(nextRound);
                break;
            }
            case 4:
            {
                ProcessPool pp = new ProcessPool();
                do
                {   
                    while(threadCount.get() !=0){};
                    pp.Test_Case4(roundCount);
                    while(learner.isAlive)
                    {
                        TimeUnit.SECONDS.sleep(3);
                    }
                    learner.isAlive = true;
                    roundCount++;
                    ProcessPool.leaderCount.set(0); //reset leadercount so that it doesn't have any previous counts from last round
                    ProcessPool.numberOfAcceptors.set(8); 
                    ProcessPool.numberOfProposers = 1;
                    AddressBook.AddressBookLookUp.clear();
                    proposer.ID_Modifier++;
                    proposer.globalAcceptorCounter.set(0); 
                }while(nextRound);
                break;
            }
            case 5:
            {
                ProcessPool pp = new ProcessPool();
                do
                {
                    pp.Test_Case5(roundCount);
                    while(learner.isAlive)
                    {
                        TimeUnit.SECONDS.sleep(3);
                    }
                    learner.isAlive = true;
                    roundCount++;
                    ProcessPool.leaderCount.set(0); //reset leadercount so that it doesn't have any previous counts from last round
                    ProcessPool.numberOfAcceptors.set(8); 
                    ProcessPool.numberOfProposers = 2;
                    AddressBook.AddressBookLookUp.clear();
                    proposer.ID_Modifier++;
                    proposer.globalAcceptorCounter.set(0);
                }while(nextRound);
                break;
            }
            case 6:
            {
                ProcessPool pp = new ProcessPool();
                do
                {
                    pp.Test_Case6(roundCount);
                    while(learner.isAlive)
                    {
                        TimeUnit.SECONDS.sleep(3);
                    }
                    learner.isAlive = true;
                    roundCount++;
                    ProcessPool.leaderCount.set(0); //reset leadercount so that it doesn't have any previous counts from last round
                    ProcessPool.numberOfAcceptors.set(7); 
                    ProcessPool.numberOfProposers = 2;
                    AddressBook.AddressBookLookUp.clear();
                    proposer.ID_Modifier++;
                    proposer.globalAcceptorCounter.set(0);
                }while(nextRound);
                break;
            }
            case 7:
            {
                ProcessPool pp = new ProcessPool();
                do
                {
                    pp.Test_Case9(roundCount);
                    while(learner.isAlive)
                    {
                        TimeUnit.SECONDS.sleep(3);
                    }
                    learner.isAlive = true;
                    roundCount++;
                    ProcessPool.leaderCount.set(0); //reset leadercount so that it doesn't have any previous counts from last round
                    ProcessPool.numberOfAcceptors.set(7); 
                    ProcessPool.numberOfProposers = 2;
                    AddressBook.AddressBookLookUp.clear();
                    proposer.ID_Modifier++;
                    proposer.globalAcceptorCounter.set(0);
                }while(nextRound);
                break;
            }
            case 8:
            {
                ProcessPool pp = new ProcessPool();
                do
                {
                    pp.Test_Case10(roundCount);
                    while(learner.isAlive)
                    {
                        TimeUnit.SECONDS.sleep(3);
                    }
                    learner.isAlive = true;
                    roundCount++;
                    ProcessPool.leaderCount.set(0); //reset leadercount so that it doesn't have any previous counts from last round
                    ProcessPool.numberOfAcceptors.set(7); 
                    ProcessPool.numberOfProposers = 2;
                    AddressBook.AddressBookLookUp.clear();
                    proposer.ID_Modifier++;
                    proposer.globalAcceptorCounter.set(0);
                }while(nextRound);
                break;
            }
            case 9:
            {
                ProcessPool pp = new ProcessPool();
                do
                {
                    pp.Test_Case7(roundCount);
                    while(learner.isAlive)
                    {
                        TimeUnit.SECONDS.sleep(3);
                    }
                    learner.isAlive = true;
                    roundCount++;
                    ProcessPool.leaderCount.set(0); //reset leadercount so that it doesn't have any previous counts from last round
                    ProcessPool.numberOfAcceptors.set(7); 
                    ProcessPool.numberOfProposers = 2;
                    AddressBook.AddressBookLookUp.clear();
                    proposer.ID_Modifier++;
                    proposer.globalAcceptorCounter.set(0);
                }while(nextRound);
                break;
            }
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, InterruptedException, IOException
    {
        //instantiate scanner object to capture user's choice of profile to run
        Scanner input = new Scanner(System.in);
        
        //print a menu
        System.out.println("Choose a paxos test profile to run: " + "\n");
        System.out.println("[TEST - INSTANT RESPONSE TIMES]");
        System.out.println("1 - Basic paxos with 1 proposer and immediate response times");
        System.out.println("2 - Basic paxos with 2 proposers proposing simultaneously and immediate response times (M1 -> M2)");
        System.out.println("3 - basic paxos with 3 proposers proposing simultaneously and immediate response times (M1 -> M3 -> M2)" + "\n");
        System.out.println("[TEST - DELAYED RESPONSE TIMES (M4 - M9 always have random response times)]");
        System.out.println("4 - 1 proposer and response times based on their councillor profiles (M2 will not reply to propose messages for first two rounds & M3 is always on a retreat in the woods)");
        System.out.println("5 - 1 proposer and response times based on their councillor profiles (M2 will not reply to propose messages for first two rounds & M3 is never on retreat in the woods)");
        System.out.println("6 - 2 propsers and response times based on their councillor profiles (proposer M2 fail after prepare() for 2 rounds)(M1 -> M2)");
        System.out.println("7 - 2 propsers and response times based on their councillor profiles (proposer M2 fail after prepare() for 2 rounds)(M2 -> M1)");
        System.out.println("8 - 2 proposer and response times based on their councillor profiles (m2 is always at work and replies instantly)");
        System.out.println("9 - 2 propsers and response times based on their councillor profiles (truly random response times)");
    
        int profile = 0;
        profile = Integer.parseInt(input.nextLine());
        ProcessInput(profile);
        input.close();
    }    
}
