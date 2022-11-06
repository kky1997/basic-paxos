import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Random;
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

    //parameterised consturctors for proposer
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

    //helper method to create an ID for the proposer, gets the quotient of the number of the proposers name 
    //so each proposer has a unique name
    //each round, the ID is incremented so that the ID of each proposer grows.
    public float createProposerID(String name)
    {
       float ID = ((float)splitName(name)/10) + ID_Modifier;
       this.proposerID = ID;
       return ID;
    }
    
    //prepare message method which will spawn threads for every acceptor and send a prepare message to each of them
    //waiting on the response from each acceptor.
    public synchronized void prepare(int count) throws IOException
    {
        proposer.proposeResponses.clear(); 
        this.AddressSet.clear(); 
        ProcessPool.numberOfAcceptors.set(ProcessPool.NUMBER_OF_ACCEPTORS);
        while(AddressBook.AddressBookLookUp.size() != ProcessPool.numberOfAcceptors.get()){} //synchronize proposer and acceptor threads, so that proposer will wait for acceptors to have added their ports to the addressbook first
        int acceptorCounter = 0;
        try 
        {
            while(!verifyMajority())
            {
                if(failedResponses.size() >= (ProcessPool.NUMBER_OF_ACCEPTORS/2)+1)
                {
                    ProcessPool.numberOfAcceptors.set(failedResponses.size());
                    break;
                }
                while(acceptorCounter < ProcessPool.numberOfAcceptors.get()) //loop for the number of acceptors that exist
                {
                    int port = AddressBook.AddressBookLookUp.get(acceptorCounter); //look up each acceptor's port in the address book
                    Thread thread = new ConnectionHandlerPrepare(port);
                    thread.start();
                    if(failedResponses.size() >= (ProcessPool.NUMBER_OF_ACCEPTORS/2)+1)
                    {
                        ProcessPool.numberOfAcceptors.set(failedResponses.size());
                        break;
                    }   
                    
                    acceptorCounter++;
                }
            }
            if(verifyMajority() == true) //the verifyMajoriy method uses prepareResponses vector to determine if the proposer has a majority + 1 of acceptors that promised
            {
                this.leader = true; //if it does, set it's leader variable to true and increment leaderCount so acceptors know how many proposers think they are leaders
                ProcessPool.leaderCount.incrementAndGet();
                ProcessPool.numberOfAcceptors.set(prepareResponses.size());
            }
            prepareFlag = true;
        } 
        catch (Exception e) 
        {
            if(e instanceof SocketTimeoutException)
            {   
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

    //Method for phase 2a propose, will spawn threads equal to number of promises (majoirty + 1) it recieved in order to send all propose messages to the acceptor threads.
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
                    System.out.println("[live or die?]" + "\n");
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
                        System.out.println(this.get_Name() + " proposer going offline without proposing \n");
                        ProcessPool.leaderCount.decrementAndGet();
                        ProcessPool.numberOfProposers--;
                        //throw new CustomException(this.get_Name() + " has gone offline"); //throw this custom exception so that if a proposer dies, it doesn't throw a timeout exception and decrement global acceptor count
                        return;
                        
                    }
                    else
                    {
                        System.out.println(this.get_Name() + " proposer not going offline \n");
                    }
                }
                while(acceptorCounter < ProcessPool.numberOfAcceptors.get())
                {
                    int port = AddressBook.AddressBookLookUp.get(acceptorCounter);
                    Thread thread = new ConnectionHandlerPropose(port);
                    thread.start();
                    thread.join();
                    acceptorCounter++;
                }
                
            }
            catch(Exception e)
            {
                if(e instanceof SocketTimeoutException)
                {
                    System.out.println("propose timeout");
                }
                else if(e instanceof CustomException)
                {
                    System.err.println(e);
                }
            }
        }
    }

    //This helper method check the input vector to see if any of the messages stored contain a nominee (accepted value)
    //looks for entries on the vector where nominees are > 0 and also the proposer ID is not the same as the proposer checking
    //this is so when the first proposer sends a value and the response is kept in the vector with the nominee, this is so that this method doesn't run every time the same
    //proposer tries to send the rest of it's propose messages to the rest of the acceptors, as this method will check if the proposer's IDs are the same,
    //if they are the same, then we know that this proposer is the first, if they aren't they same, then we know another proposer (with smaller ID) has already
    //proposed a value previously, hence my ID is different and nominee is not 0.
    public boolean checkPreviousProposal(Vector<Message> responses)
    {
        for(int i = 0; i < responses.size(); i++)
        {
            if(responses.get(i).nominee > 0 && !responses.get(i).name.equalsIgnoreCase(get_Name())) 
            {   
                return true;
            }
        }
        return false;
    }

    //method to verify if proposer has majority + 1 acceptors by counting the number of prepare responses in the vector and seeing how many contain the
    //the same ID as the current proposer. If ID is equal, then we know that this proposer has recieved a promise from an acceptor.
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

    //This is the threaded class that handles threads for every prepare message to be sent to each acceptor
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

        //run method for thread to execute
        public synchronized void run()
        {
            try
            {
                
                proposerSocket = new Socket("127.0.0.1", port);
                proposerSocket.setSoTimeout(2500);
                output = new ObjectOutputStream(proposerSocket.getOutputStream());
                input = new ObjectInputStream(proposerSocket.getInputStream());
                
                //phase 1a - send prepare message with ID
                Message prepare = new Message(proposerID);
                output.writeUnshared(prepare);
                Message promise = (Message) input.readObject();

                System.out.println(get_Name() + " recieves response from acceptor " + promise.name + ": " + promise.proposer_ID + " " + promise.response);

                if(!promise.response.equals("failed")) //if they respond with failed (this means a higher ID proposer already proposed, don't add that promise to the prepareResponses vector)
                {
                    //only need majority + 1 promises
                    if(prepareResponses.size() != ProcessPool.NUMBER_OF_ACCEPTORS/2 + 1)
                    {
                        prepareResponses.add(promise); //else, add the promise to the vector
                    }
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

    //this is the threaded class responsible for creating threads to handle each Propose message to every acceptor thread
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

         //run method for thread to execute
        public synchronized void run()
        {
            try
            {
                if(leader == true)
                {
                    System.out.println(get_Name() +" is PROPOSING TO port " + port);
                    proposerSocket = new Socket("127.0.0.1", port);
                    proposerSocket.setSoTimeout(3000); 
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
                            Message response = (Message) input.readObject(); 
                        }
                        else //if no message on the static vector contains a previously accepted value, then m1-m3 will just nominate themselves
                        {
                            Message propose = new Message(proposerID, splitName(get_Name()), get_Name()); //send their ID and who they nominate (in this case it is themselves)
                            output.writeObject(propose);
                            output.flush();
                            Message response = (Message) input.readObject();
                            proposeResponses.add(response);
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
                            Message response = (Message) input.readObject();
                        }
                        else
                        {
                            Message propose = new Message(proposerID, 5, get_Name());
                            output.writeObject(propose);
                            output.flush();
                            Message response = (Message) input.readObject();
                            proposeResponses.add(response);
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
                }
                else if(e instanceof SocketException)
                {
                }
                else if (e instanceof EOFException)
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
}
