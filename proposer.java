import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;

import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

public class proposer extends Member
{
    public static float ID_Modifier = 0;
    public float proposerID = 0;
    public boolean leader = false;
    public int numberOfResponses = 0;
    public Vector<Message> prepareResponses = new Vector<Message>(ProcessPool.NUMBER_OF_ACCEPTORS/2 + 1);
    public Vector<Message> failedResponses = new Vector<Message>(ProcessPool.NUMBER_OF_ACCEPTORS/2);
    volatile public static Vector<Message> proposeResponses = new Vector<Message>();
    volatile public CopyOnWriteArraySet<Integer> AddressSet = new CopyOnWriteArraySet<Integer>();
    public int proposeOrNot = 0;
    public boolean prepareFlag = false;
    
    public static AtomicInteger globalAcceptorCounter = new AtomicInteger(0);

    public proposer(String name)
    {
        super(name);
        super.set_Proposer(true);
        proposerID = createProposerID(this.get_Name()); //create proposerID based on their membername
    }

    public proposer(String name, int rt, int selector)
    {
        super(name, rt);
        super.set_Proposer(true);
        proposerID = createProposerID(this.get_Name()); //create proposerID based on their membername
        this.proposeOrNot = selector;
    }

    public proposer(String name, int rt)
    {
        super(name, rt);
        super.set_Proposer(true);
        proposerID = createProposerID(this.get_Name()); //create proposerID based on their membername
    }
    public proposer(String name, boolean status, boolean desire, boolean proposer, boolean learner, boolean acceptor, int rt) 
    {
        super(name, status, desire, proposer, learner, acceptor, rt);
        proposerID = createProposerID(this.get_Name()); //create proposerID based on their membername
    }

    public float createProposerID(String name)
    {
       float ID = ((float)splitName(name)/10) + ID_Modifier;
       this.proposerID = ID;
       //ID_Modifier++;
       return ID;
    }
    
    //prepare message method
    //NOTE TO SELF, MAYBE MAKE PREPARE() a threaded class and have threads to handle each prepare request
    //so that acceptors can accept based on the reply speeds of their profiles
    //this way, we can constantly check if we have majority + 1 yet, and if we do, we just close all not finish connections
    //and proceed to propose.
    public synchronized void prepare(int count) throws IOException
    {
        proposer.proposeResponses.clear(); //!!!!!!!
        this.AddressSet.clear(); // !!!!!!!!!!!!!!!!!
        ProcessPool.numberOfAcceptors.set(ProcessPool.NUMBER_OF_ACCEPTORS); //- M3tracker;
        while(AddressBook.AddressBookLookUp.size() != ProcessPool.numberOfAcceptors.get()){} //synchronize proposer and acceptor threads, so that proposer will wait for acceptors to have added their ports to the addressbook first
        int acceptorCounter = 0;
        try 
        {
            //System.out.println(this.get_Name() + " is calling prepare method");
            while(!verifyMajority())
            {
                if(failedResponses.size() >= (ProcessPool.NUMBER_OF_ACCEPTORS/2)+1)
                {
                    ProcessPool.numberOfAcceptors.set(failedResponses.size());
                    break;
                }
                while(acceptorCounter < ProcessPool.numberOfAcceptors.get()) //loop for the number of acceptors that exist
                {
                    //System.out.println(this.get_Name() + " is inside thread creation prepare loop");

                    int port = AddressBook.AddressBookLookUp.get(acceptorCounter); //look up each acceptor's port in the address book
                    Thread thread = new ConnectionHandlerPrepare(port);


                    thread.start();
                    //thread.join();
                    if(failedResponses.size() >= (ProcessPool.NUMBER_OF_ACCEPTORS/2)+1)
                    {
                        ProcessPool.numberOfAcceptors.set(failedResponses.size());
                        break;
                    }   
                    
                    acceptorCounter++;
                }
            }

            //System.out.println(this.get_Name() + " I am outside both prepare loops");!!!!!!!!!!!!!!!!!!!!!

            if(verifyMajority() == true) //the verifyMajoriy method uses prepareResponses vector to determine if the proposer has a majority + 1 of acceptors that promised
            {
                //System.out.println("I have majority");!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                this.leader = true; //if it does, set it's leader variable to true and increment leaderCount so acceptors know how many proposers think they are leaders
                ProcessPool.leaderCount.incrementAndGet();
                ProcessPool.numberOfAcceptors.set(prepareResponses.size());
                
                
            }
            
            prepareFlag = true;
            //System.out.println(this.get_Name() + " am I leader? " + this.leader);

        } 
        catch (Exception e) 
        {
            //this logic here allows paxos to continue if an acceptor fails (eg. m3 is at a retreat in the woods)
            //this prepare() method has a readObject timer set for 11seconds, if those 11 seconds expire due to an acceptor being unavailable
            //it will trigger an instance of SocketTimeOutException, and we can enter the below if statement
            //this if statement saves the current acceptor counter which is used to traverse the addressbook vector to get each acceptor's address.
            //hence we save the current number, remove the acceptor that didn't respond to the prepare and then decrement the global number of acceptors (eg. m3 is now not part of the vote)
            //then we recursively call the prepare() method and start it from the last acceptorCounter number, this means it will continue to contact the remaining acceptors which have not been
            //send a prepare message yet, this is done through the addressbook which will start from the previous acceptorCounter (since we removed the last acceptor, now all elements shifted to left),
            //we can simply just start from where the proposer was up to last in the address book vector.
            if(e instanceof SocketTimeoutException)
            {   
                /* 
                System.out.println("socket timeout prepare");
                int currentAcceptorPointer = acceptorCounter;
                AddressBook.AddressBookLookUp.remove(currentAcceptorPointer);
                ProcessPool.numberOfAcceptors.decrementAndGet();
                prepare(currentAcceptorPointer);*/
            }
            else if(e instanceof ArrayIndexOutOfBoundsException)
            {
                System.out.println(e);
                e.printStackTrace();
            }
            else
            {
                System.out.println("PROPOSER ERROR");
                System.out.println(e);
                e.printStackTrace();
            }
        }
    }

    //phase 2a propose
    public synchronized void propose(int counter) throws IOException //throws IOException, ClassNotFoundException
    {
        if(this.leader == true) //proposers need to be nominated leader for this to happen
        {
            int acceptorCounter = counter;
            try
            {
                if(this.get_Name().equalsIgnoreCase("M2") && this.get_ResponseTime() == 3 && this.get_Proposer() == true||
                this.get_Name().equalsIgnoreCase("M3") && this.get_Proposer() == true && this.get_ResponseTime() != 1)
                {
                    System.out.println("live or die?" + "\n");
                    int selector = 0;
                    if(proposeOrNot == 0)
                    {
                        Random random = new Random();
                        selector = random.nextInt(3) + 1;
                    }
                    else if(proposeOrNot == 1)
                    {
                        selector = 1;
                    }
                    else if(proposeOrNot == 2)
                    {
                        selector = 2;
                    }

                    if(selector == 1)
                    {
                        System.out.println(this.get_Name() + " proposer going offline");
                        ProcessPool.leaderCount.decrementAndGet();
                        ProcessPool.numberOfProposers--;
                        //throw new CustomException(this.get_Name() + " has gone offline"); //throw this custom exception so that if a proposer dies, it doesn't throw a timeout exception and decrement global acceptor count
                        return;
                        
                    }
                    else
                    {
                        System.out.println(this.get_Name() + " proposer not going offline");
                    }
                }
                //CopyOnWriteArrayList<Integer> arrayList = new CopyOnWriteArrayList<Integer>(AddressSet); //construct vector with hashset's content
                while(acceptorCounter < ProcessPool.numberOfAcceptors.get())
                {
                    int port = AddressBook.AddressBookLookUp.get(acceptorCounter);//prepareResponses.get(acceptorCounter).port;//arrayList.get(acceptorCounter);
                    Thread thread = new ConnectionHandlerPropose(port);
                    thread.start();
                    thread.join();
                    acceptorCounter++;
                }
                
            }
            catch(Exception e)
            {
                //????????!?!?!?!?
                if(e instanceof SocketTimeoutException)
                {
                    //same logic as prepare(), if an acceptor thread fails before sending back an accept message to the proposer (afte 11 seconds) it will cause a timeout exception.
                    //then we will remove that thread out of the global numberOfAcceptor pool, as well as it's address removed from the address book so it won't be contacted again
                    //then we recursively call propose and feed it the number of the next thread so it can continue to contact the rest of the threads and solicite the accepted messages.
                    System.out.println("propose timeout");
                    
                    //int currentAcceptorPointer = acceptorCounter;
                    //AddressBook.AddressBookLookUp.remove(currentAcceptorPointer);
                    //ProcessPool.numberOfAcceptors--;
                    //propose(currentAcceptorPointer);
            
                    
                }
                else if(e instanceof CustomException)
                {
                    //goneOffline = true;
                    System.err.println(e);
                }
            }
        }
    }

    //check the input vector to see if any of the messages stored contain a nominee (accepted value)
    public boolean checkPreviousProposal(Vector<Message> responses)
    {
        for(int i = 0; i < responses.size(); i++)
        {
            //changed this if statement so that it looks for entries on the vector where nominees are > 0 and also the proposer ID is not the same as the proposer checking
            //this is so when the first proposer sends a value and the response is kept in the vector with the nominee, this method doesn't run every time the same
            //proposer tries sends the rest of it's propose messages to the rest of the acceptors, as this method will check if the proposer's IDs are the same,
            //if they are the same, then we know that this proposer is the first, if they aren't they same, then we know another proposer (with smaller ID) has already
            //proposed a value previously, hence my ID is different and nominee is not 0.
            if(responses.get(i).nominee > 0 && !responses.get(i).name.equalsIgnoreCase(get_Name())) 
            {   
                return true;
            }
        }
        return false;
    }

    //if m3 is in the woods, need to omit their port from the address book so that they aren't by other proposers
    public void OmitFromAddressBook()
    {
        for(int i = 0; i < AddressBook.AddressBookLookUp.size(); i++)
        {
            if(AddressBook.AddressBookLookUp.get(i) == 8003)
            {
                AddressBook.AddressBookLookUp.remove(i);
            }
        }
    }

    //method to verify if proposer has majority + 1 acceptors
    public boolean verifyMajority()
    {
        int validAcceptors = 0;
        for(int i = 0; i < prepareResponses.size(); i++)
        {
            if(prepareResponses.get(i).proposer_ID == this.proposerID)
            {
                validAcceptors++;
            }
        }
        if(validAcceptors > ProcessPool.NUMBER_OF_ACCEPTORS/2) //if number of validacceptors is > half of all acceptors, then we have majority +1
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    //helper method to randomly decide if M2 and M3 will go offline after proposing DEPRECATED NOT USING ATM
    public boolean LiveOrDie()
    {
        //check if the proposer is actually either M2 or M3 (NOTE that M2 will only have a chance to not reply if they aren't working (indicated by respose time 3))
        System.out.println("live or die?");
        Random random = new Random();
        
        if(random.nextBoolean())
        {
            ProcessPool.leaderCount.decrementAndGet();
            System.out.println(this.get_Name() + " as proposer has died");
            return true;
        }
        else
        {
            return false;
        }
    
    }

    public class ConnectionHandlerPrepare extends Thread
    {
        int port = 0;
        ObjectInputStream input;
        ObjectOutputStream output;
        Socket proposerSocket;

        public ConnectionHandlerPrepare(int port)
        {
            this.port = port;
        }

        public synchronized void run()
        {
            try
            {
                
                proposerSocket = new Socket("127.0.0.1", port);

                proposerSocket.setSoTimeout(2500); //set a 11second timeout for reads

                output = new ObjectOutputStream(proposerSocket.getOutputStream());
                input = new ObjectInputStream(proposerSocket.getInputStream());
                
                //phase 1a - send prepare message with ID
                Message prepare = new Message(proposerID); //send proposer ID (just the councilors id)
                output.writeUnshared(prepare);
                Message promise = (Message) input.readObject(); //read the promise made by the acceptor

                System.out.println("Response from acceptor: " + promise.proposer_ID + " " + promise.response); //!!!!!!!!!!!!!!!!!!!!!!

                if(!promise.response.equals("failed")) //if they respond with failed (this means a higher ID proposer already proposed, don't add that promise to the prepareResponses vector)
                {
                    //only need majority + 1 promises
                    if(prepareResponses.size() != ProcessPool.NUMBER_OF_ACCEPTORS/2 + 1)
                    {
                        
                        prepareResponses.add(promise); //else, add the promise to the vector

                        //System.out.println("promise added for " + get_Name());  //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    }

                    /*if(AddressSet.size() != prepareResponses.size())
                    {
                        AddressSet.add(port);
                    }*/
                    
                    //System.out.println(port + " added to set");  //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                }
                else
                {
                   
                    failedResponses.add(promise);
                    
                }
                globalAcceptorCounter.getAndIncrement(); //this is used for the acceptor spinlock, only incremented in prepare not propose since propose is after the spinlock in acceptor
            }
            catch(Exception e)
            {
                
                if(e instanceof SocketTimeoutException)
                {
                    //if any acceptor never sends a reply (died or taking too long), will get a readtimeout
                    //this will cause the proposer thread responsbile for the acceptor that died to be stopped
                    //must still increment the counter, signify that M3 has died and also increase the globalAcceptorCount
                    //so that subsequent propose calls can still be made with the now different ProcessPool values
                    System.out.println("Interrupting proposer thread responsible for m3");
                    currentThread().interrupt();

                    //M3tracker++;

                    globalAcceptorCounter.getAndIncrement();

                }
                else if(e instanceof SocketException)
                {

                }
                else
                {
                    System.out.println("SOCKET HANDLER PREPARE FAILING");
                    e.printStackTrace();
                }
            }
            finally
            {
                try 
                {
                    proposerSocket.close();
                } 
                catch (Exception e) 
                {
                    if(e instanceof NullPointerException)
                    {

                    }
                    else
                    {
                        e.printStackTrace();
                    }
                    
                }
            }
        }
    }

    public class ConnectionHandlerPropose extends Thread
    {  
        int port = 0;
        ObjectInputStream input;
        ObjectOutputStream output;
        Socket proposerSocket;

        public ConnectionHandlerPropose(int port )
        {
            this.port = port;
        }

        public synchronized void run()
        {
            try
            {
                if(leader == true)
                {
                    System.out.println(get_Name() +" is PROPOSING TO port " + port);

                    proposerSocket = new Socket("127.0.0.1", port);
                    

                    //System.out.println(get_Name() + " is connected? " + proposerSocket.isConnected()); !!!!!!!!!!!!!!!!!!!!!!!!!
                    

                    proposerSocket.setSoTimeout(2500); //set a 11second timeout for reads //??!!!!!!!!!!!!!!!!!!!!!! PROBABLY DOESN"T MATTER changed from 11s to 2.7

                    

                    output = new ObjectOutputStream(proposerSocket.getOutputStream());
                    input = new ObjectInputStream(proposerSocket.getInputStream());

                    
                    
                    //if councilors are M1, 2, or 3, then they are keen to nominate themselves
                    if(get_Name().toLowerCase().equals("m1") || get_Name().toLowerCase().equals("m2") || get_Name().toLowerCase().equals("m3"))
                    {
                        //if static proposeResponses vector contains an accpeted value already
                        //then we must loop through the vector to the first instance of this accepted value and accept that as the value
                        //which this proposer till propose
                        if(checkPreviousProposal(proposeResponses))
                        {
                            int previousProposal = 0; //set the accpeted value to this variable
                            for(int i = 0; i < proposeResponses.size(); i++)
                            {
                                if(proposeResponses.get(i).nominee > 0)
                                {
                                    previousProposal = proposeResponses.get(i).nominee;
                                    break;
                                }
                            }
                            Message propose = new Message(proposerID, previousProposal, get_Name()); //send the message containing the previous proposal by another proposer
                            output.writeObject(propose);
                            output.flush();
                            //System.out.println("prevous proposal detected for nominee:  " + get_Name());
                            Message response = (Message) input.readObject(); //acceptor will send back it's acknowledgment
                            //System.out.println(response.nominee + " the single digit is first if of propose");
                        }
                        else //if no message on the static vector contains a previously accepted value, then m1-m3 will just nominate themselves
                        {
                            Message propose = new Message(proposerID, splitName(get_Name()), get_Name()); //send their ID and who they nominate (in this case it is themselves)

                            //System.out.println(propose.name);

                            output.writeObject(propose);
                            output.flush();
                            //System.out.println("after write object 1");
                            Message response = (Message) input.readObject();
                            proposeResponses.add(response);
                            //System.out.println(response.nominee + " " + response.response + " the single digit is 2nd else of propose");
                        }
                        
                    }
                    else //m4-m9 will do the same except they will nominate someone other than themselves.
                    {
                        if(checkPreviousProposal(proposeResponses))
                        {
                            int previousProposal = 0;
                            for(int i = 0; i < proposeResponses.size(); i++)
                            {
                                if(proposeResponses.get(i).nominee > 0)
                                {
                                    previousProposal = proposeResponses.get(i).nominee;
                                    break;
                                }
                            }
                            
                            Message propose = new Message(proposerID, previousProposal, get_Name());
                            output.writeObject(propose);
                            output.flush();
                            //System.out.println("prevous proposal detected and sent from " + get_Name());
                            Message response = (Message) input.readObject();
                            //System.out.println("accept message from acceptors: " + get_Name() + " " + response.nominee);
                        }
                        else
                        {
                            Message propose = new Message(proposerID, 5, get_Name());
                            output.writeObject(propose);
                            output.flush();
                            //System.out.println("after write object 2");
                            Message response = (Message) input.readObject();
                            proposeResponses.add(response);
                            //System.out.println("accept message from acceptors: " + get_Name() + " " + response.nominee);
                        }
                        
                    }
                }     
            }
            catch(Exception e)
            {
                if(e instanceof SocketTimeoutException)
                {
                    System.out.println("M2 must have died");
                }
                else if(e instanceof ConnectException)
                {
                    System.out.println("connect exception proposer");
                    //e.printStackTrace();
                }
                else if(e instanceof SocketException)
                {

                }
                else
                {
                    e.printStackTrace();
                }
            }
            finally
            {
                try 
                {
                    //System.out.println(get_Name() + " proposer is going offline");
                    proposerSocket.close();
                } 
                catch (Exception e) 
                {
                    if(e instanceof NullPointerException)
                    {

                    }
                    else
                    {
                        e.printStackTrace();
                    }
                }
            }
        }


    }
   

   //MAYBE HAVE PREPARE AND PROPOSE IN A MAIN IN THIS CLASS, THAT WAY DON'T NEED IT IN THE PROCESS POOL

}
