import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class acceptor extends Member
{
    ServerSocket acceptorServerSocket;
    volatile public int accepted_Value = 0; //accepted value
    public ObjectInputStream input;
    public ObjectOutputStream output;
    public Socket acceptorSocket;
    public float largestProposerID = 0;
    public static boolean checkM3 = false; //check to see if acceptors have already checked if M3 is in the woods today, so acceptors only check once to see if he is in woods or not
    public int proposeSocketTimeOut = 0;
    public static AtomicBoolean m3InWoods = new AtomicBoolean(false);
    public int prepareSocketTimeOutServerSocket = 0;
    public int prepareSocketTimeOutSocket = 0;

    public int caseSelectorM3 = 0;
    public int caseSelectorM2 = 0;

    public acceptor(String name)
    {
        super(name);
        super.set_Acceptor(true);
    }
    
    public acceptor(String name, int rt, int timeout)
    {
        super(name, rt);
        super.set_Acceptor(true);
        this.proposeSocketTimeOut = timeout;
    }

    public acceptor(String name, int rt, int timeout, int selectorM3, int selectorM2)
    {
        super(name, rt);
        super.set_Acceptor(true);
        this.proposeSocketTimeOut = timeout;
        this.caseSelectorM3 = selectorM3;
        this.caseSelectorM2 = selectorM2;
    }


    public acceptor(String name, boolean status, boolean desire, boolean proposer, boolean learner, boolean acceptor, int rt) 
    {
        super(name, status, desire, proposer, learner, acceptor, rt);
        super.set_Acceptor(true);
    }

    @Override
    public void run()
    {
        //System.out.println(getPort());
        try
        {
            startAcceptor(getPort());
            //System.out.println(this.get_Name() + " acceptor thread is closing");

        }
        catch(Exception e)
        {
            System.err.println(e);
            e.printStackTrace();
        }
        finally
        {
            try 
            {
                acceptorServerSocket.close();
            } catch (Exception e) 
            {
                if(e instanceof ConnectException)
                {

                }
                else
                {
                    e.printStackTrace();
                }
                
            }
        }
    }

    public int getPort()
    {
        String name = get_Name(); //using own get name method
        int port = splitName(name) + 8000;
        return port;
    }

    //check if proposer ID is biggest and send promise or no promise back
    public void CheckProposerID(Message prepare)
    {
        try
        {
            float previousLargestProposal_ID = 0;
            if(prepare.proposer_ID <= this.largestProposerID) //if this proposers ID is smaller
            {

                Message failed = new Message("failed");
                failed.proposer_ID = prepare.proposer_ID;
                output.writeUnshared(failed);
                output.flush();
              
            }
            else if(prepare.proposer_ID > this.largestProposerID) //if new proposer number bigger than previous
            {
                previousLargestProposal_ID = this.largestProposerID;
                this.largestProposerID = prepare.proposer_ID;
                if(this.accepted_Value!=0) //check if already accepted a value from previous proposer, if blank
                {
                    //respond with (ID, accepted value, accepted_ID)
                    Message promise = new Message(prepare.proposer_ID, accepted_Value, previousLargestProposal_ID);
                    promise.port = getPort();
                    promise.response = "promise";
                    output.writeUnshared(promise);
                    output.flush();
                }
                else
                {
                    //else just respond with ID of largest proposer
                    Message promise = new Message(prepare.proposer_ID); //respond with proposer's ID
                    promise.port = getPort();
                    promise.response = "promise";
                    output.writeUnshared(promise);
                    output.flush();
                }
                
            }
            
        }
        catch(Exception e)
        {
            if(e instanceof ConnectException)
            {
                
            }
            else
            {
                e.printStackTrace(); ///!!!!!!!!!!!!!!!!!!!!! WASn"T PRINTING EXCEPTION BEFORE
            }
        }
    }

    public void informLearner(Message accepted)
    {
        try 
        {
            acceptorSocket = new Socket("127.0.0.1", 9000);
            output = new ObjectOutputStream(acceptorSocket.getOutputStream());
            input = new ObjectInputStream(acceptorSocket.getInputStream());
            output.writeUnshared(accepted);
        } 
        catch (Exception e) 
        {
            if(e instanceof ConnectException)
            {
                
            }
            else
            {
                System.err.println(e);
                e.printStackTrace();
            }
        }
    }


    
    public synchronized void startAcceptor(int port) throws InterruptedException, IOException
    {
        int proposerCounter = 0;
        int leaderCounter = 0;
        boolean alive = true;
        try 
        {
            AddressBook.AddressBookLookUp.add(port); //add each acceptors port to the address book
            acceptorServerSocket = new ServerSocket();
            acceptorServerSocket.setReuseAddress(true); //this is to ensure that even if the socket is in a TIME_WAIT state, even after closing(), it won't throw bind already in use exception
            acceptorServerSocket.bind(new InetSocketAddress(port));
            

            //SET TIMEOUT so that during the prepare/promise phase, acceptors that aren't contacted (because majority already reached)
            //will not have their bind addresses in use by the next round
            acceptorServerSocket.setSoTimeout(8000); //WAS 10SECONDS
            
            while(ProcessPool.numberOfProposers > proposerCounter) //allow each proposer to connect 
            {
                //need to accept new socket connection each time and create now stream, hence
                //below 3 lines must be in the loop
                //System.out.println(AddressBook.AddressBookLookUp.firstElement());

                //phase 1b - acceptor promise
                acceptorSocket = acceptorServerSocket.accept();

                //System.out.println(this.get_Name() + " after accept"); !!!!!!!!!!!!!!!!!!!!!!!!

                acceptorSocket.setSoTimeout(6000); //WAS 6SECONDS

                output = new ObjectOutputStream(acceptorSocket.getOutputStream());
                input = new ObjectInputStream(acceptorSocket.getInputStream());
                //System.out.println("-----acceptor in while loop");
                //phase 1b acceptors get prepare message
                Message prepare = (Message) input.readObject();
                //System.out.println("prepare message recieved from proposerID: " + prepare.proposer_ID);

                
                if(this.get_ResponseTime() == 2 && !this.get_Name().equalsIgnoreCase("m3") && this.get_Acceptor() == true)
                {
                    Thread.sleep(1000); //medium sleep for 3s
                }
                else if(this.get_ResponseTime() == 3 && !this.get_Name().equalsIgnoreCase("m2") && this.get_Acceptor() == true)
                {
                    Thread.sleep(2000); //float sleep
                }

                //generate a random number to see if M3 is on wood retreat, this will only apply to M3 if they are an acceptor not a proposer
                //hence we check if they were constructed as an acceptor.
                else if(this.get_ResponseTime() == 2 && this.get_Name().equalsIgnoreCase("m3") && this.get_Acceptor() == true && checkM3 == false)
                {
                    System.out.println("GENERATING RANDOM NUMBER AND SEEING IF M3 is in woods");
                    int selector = 0;
                    if(caseSelectorM3 == 0)
                    {
                        Random random = new Random();
                        selector = random.nextInt(3)+1;
                    }
                    else if(caseSelectorM3 == 1)
                    {
                        selector = 1;
                    }
                    else if(caseSelectorM3 == 2)
                    {
                        selector = 2;
                    }
                    
                    if(selector == 1) //number is 1, break and never send a message back to proposer (m3 has gone on a retreat to the woods)
                    {
                        System.out.println("M3 in woods today"); //if m3 is in the woods, they won't reply to any requests
                        checkM3 = true;

                        //OmitFromAddressBook();
                        m3InWoods.set(true);
                        Thread.sleep(2700);
                        
                        return;
                    }
                    else
                    {
                        System.out.println("M3 is not in the woods today");
                        Thread.sleep(1000); //else if selector is 2, m3 just replies at a medium speed
                        checkM3 = true; //set this to indicate that acceptors have checked and he is not in the woods today
                    }
                }

                //this will take the prepare message and will either send promise back, or a failed message
                //if there has already been a promise made to a lower ID proposer and an accepted value,
                //this method will just send back ID

                CheckProposerID(prepare); 

                proposerCounter++;
            }

            //WHILE LOOP mutex, only if there is a single proposer, need threads to wait for every prepare message to be sent and for the proposer to
            //verify that it has majority, then do we allow the acceptor threads to continue to the below phase 2b loop.
            //this mutex will check if there is indeed only 1 proposer, and also if the proposer is done sending prepare messages to all numberofAcceptors
            //it also checks if the single proposer has delcared itself leader yet before it moves on.
            //this while loop continuously sleeps threads for 500ms till they are all at the same stage, where they can all progress to the next while loop 
            //for phase2b and the proposer can have time to start phase2a.
        
        while(/*ProcessPool.numberOfProposers <= 1 && */ proposer.globalAcceptorCounter.get() != ProcessPool.numberOfAcceptors.get() && ProcessPool.leaderCount.get() == 0 /*&& proposer.prepareThreadCounter.get() != 0*/)
        {
            Thread.sleep(200);
        }
        
            //check how many proposers think they are leaders, if largest ID was first, then there will be no oder leaders as every other proposer with smaller ID
            //would have failed the prepare stage, however if largest ID was last proposer to propose, then all prevous proposers will think they are the leader
            //hence we allow each of them to connect again

            
            //System.out.println(this.get_Name() + "'s' pp.leadercount right before phase2b while loop: " + ProcessPool.leaderCount); !!!!!!!!!!!!!!!!!!!!!!!!!!

            //phase 2b
            while(ProcessPool.leaderCount.get() > leaderCounter) 
            {
                //System.out.println(this.get_Name() + " IS IN WHILE 2 ACCEPTOR"); !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                
                acceptorSocket = acceptorServerSocket.accept();

                //System.out.println(this.get_Name() + " has connected");

                acceptorServerSocket.setSoTimeout(proposeSocketTimeOut); // set to 8 for test case 6 where m4-m9 have 2s response time (should be most degenerate case)

                acceptorSocket.setSoTimeout(proposeSocketTimeOut); // set to 8 for test case 6 where m4-m9 have 2s response time (should be most degenerate case)
                
                output = new ObjectOutputStream(acceptorSocket.getOutputStream());
                input = new ObjectInputStream(acceptorSocket.getInputStream());
                
                //phase 2b
                Message proposition = (Message) input.readObject(); //read their message containing (ID, proposition)
                if(this.accepted_Value == 0) //if no accepted value yet
                {
                    accepted_Value = proposition.nominee; //set the accepted_value to whatever the first proposer proposes. (this never needs to change as all other proposers will use this value)
                }

                //System.out.println("acceptor recieves accept message this is nominee: " + proposition.nominee);!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                

                if(this.get_ResponseTime() == 2 && !this.get_Name().equalsIgnoreCase("m3") && this.get_Acceptor() == true)
                {
                    Thread.sleep(1000); //medium sleep for 3s
                }
                else if(this.get_ResponseTime() == 3 && !this.get_Name().equalsIgnoreCase("m2") && this.get_Acceptor() == true)
                {
                    Thread.sleep(2000); //float sleep
                }

                //here we generate a random number to see if m2 will reply to this or not, since they have a bad internet connection and don't always reply to all emails
                //we check if response time is 3 (they are not working) and also their name is "m2"
                //if they are working, they respond to everything instantly and reply to every email, so we can skip this.
                //NOTE that this will only affect members who are ACCEPTORS, hence we check if they are constructed as acceptors
                else if(this.get_ResponseTime() == 3 && this.get_Name().equalsIgnoreCase("m2") && this.get_Acceptor() == true)
                {
                    System.out.println("GENERATING RANDOM NUMBER AND SEEING IF M2 WILL REPLY (DODGY INTERNET)");

                    int selector = 0;
                    if(caseSelectorM2 == 0)
                    {
                        Random random = new Random();
                        selector = random.nextInt(3)+1;
                    }
                    else if(caseSelectorM2 == 1)
                    {
                        selector = 1;
                    }
                    else if(caseSelectorM2 == 2)
                    {
                        selector = 2;
                    }

                    if(selector == 1) //number is 1, break and never send a message back to proposer (m2 sometimes doesn't reply to all messages)
                    {
                        System.out.println("M2 randomly decided not to reply to propose message");

                        //we set this alive boolean bit to false, this is because although below we interrupt this thread and break the while loop, outside the loop we still
                        //have the accepted message that is sent to the learner. So we must indicate the thread is not alive so that it doesn't continue to send an accepted message 
                        //to the learner and cause the learner to block since it's not reciving anything.
                        alive = false; 
                        //this.interrupt();
                        return;
                        //break;
                    }
                    else
                    {   
                        System.out.println("M2 is just replying slowly");
                        Thread.sleep(2000); //else if selector is 2, m2 just replies at a slow speed
                    }
                }
                Message accepted = new Message(proposition.proposer_ID, proposition.nominee, proposition.name);
                output.writeUnshared(accepted);
                output.flush();

                //System.out.println("CURRENT LEADER COUNT IN acceptor WHILE 2: " + ProcessPool.leaderCount); //!!!!!!!!!!!!

                leaderCounter++;
                //System.out.println("THIS IS LEADERCOUNTER LOCAL TO ACCEPTOR " + this.get_Name() + " " + leaderCounter); !!!!!!!!!!!!!
            }

            Message accpeted = new Message(largestProposerID, accepted_Value); 
            informLearner(accpeted);
            
        } 
        catch (Exception e) 
        {
            if(e instanceof InterruptedException)
            {
                System.out.println("M3 is in the woods, they are not part of this vote");
            }
            else if(e instanceof SocketTimeoutException)
            {
                //System.out.println(this.get_Name() + " acceptor serversocket timed out");
            }
            else if(e instanceof EOFException)
            {
            }
            else if(e instanceof ConnectException)
            {

            }
            else if(e instanceof BindException)
            {
                System.out.println("This acceptor already bound");
            }
            else
            {
                System.out.println(e + " ACCEPTOR IS FAILING");
                e.printStackTrace();
            }
        }
        finally
        {
            Driver.threadCount.getAndDecrement();
            acceptorServerSocket.close(); //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        }
    }


    

    
}
