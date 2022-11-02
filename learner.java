import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Vector;

public class learner extends Member
{
    ServerSocket learnerServerSocket;
    //public static AtomicInteger tally = new AtomicInteger();
    public static Vector<Integer> tallyVector = new Vector<Integer>();
    public static boolean isAlive = true;
    public String expectedResult = "";


    public learner(String name)
    {
        super(name);
        super.set_Leaner(true);
    }
    
    public learner(String name, String expected)
    {
        super(name);
        super.set_Leaner(true);
        this.expectedResult = expected;
    }

    @Override
    public void run() 
    {
        try{
            startLearner();
            if(!tallyVector.isEmpty())
            {
                String winner = AnnounceWinner(tallyVector);
                System.out.println();
                System.out.println(winner);
                if(!winner.equalsIgnoreCase("vote failed"))
                {
                    Driver.nextRound = false;
                    isAlive = false;
                    System.out.println("The expected result is: " + expectedResult);
                    System.exit(0);
                }
            }
        }
        catch(Exception e)
        {
            System.err.println(e);
            e.printStackTrace();
        }
        finally
        {
            tallyVector.clear();
        }
        
        
    
    }

    public void startLearner() throws ClassNotFoundException, InterruptedException, IOException
    {
        int acceptorCounter = 0;
        try 
        {
            //while loop which checks input (will make input send an object that)
            //contains who they vote for and also their thread number
            //while loop ends when thread number = number of threads created
            //so only after all threads have voted
            //inputstream can end and the result of vote can be announced
            //can spawn threads again like A2 to handle each acceptor connecting
            learnerServerSocket = new ServerSocket(9000);
            learnerServerSocket.setSoTimeout(35000); //WAS 30SECONDS
            /*
            learnerSocket = learnerServerSocket.accept();
            output = new ObjectOutputStream(learnerSocket.getOutputStream());
            input = new ObjectInputStream(learnerSocket.getInputStream());
            String response = (String) input.readObject();
            System.out.println(response);*/
            while(acceptorCounter < ProcessPool.numberOfAcceptors.get()) //loop for the number of acceptors that are present in the system
            {
                System.out.println("learner number of votes: " + acceptorCounter + " out of " + (ProcessPool.NUMBER_OF_ACCEPTORS/2+1));
                Thread thread = new ConnectionHandlerLearner(learnerServerSocket.accept()); //start thread to handle each acceptors message writes/reads
                
                //System.out.println(thread.getId()); Threads assigned different ID even though local variable "thread" is same within scope of the while loop
                
                thread.start(); //each thread will start and join so that they are executed sequentially
                thread.join();
                if(tallyVector.size() >= ((ProcessPool.NUMBER_OF_ACCEPTORS/2)+1))
                {
                    break;
                }
                //System.out.println(tallyVector.size());
                acceptorCounter++;
            }
            /*for(int i = 0; i < tallyVector.size(); i ++)
            {
                System.out.println(tallyVector.get(i));
            }*/
            
        } 
        catch (Exception e) 
        {
            if(e instanceof SocketTimeoutException)
            {
                System.out.println("learner timedout");
                //learnerServerSocket.close(); // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                
            }
            else
            {
                System.out.println("LEARNER ERROR");
                System.out.println(e);
                e.printStackTrace();
            }
        }
        finally
        {
            isAlive = false;
            Driver.nextRound = true;
            learnerServerSocket.close(); //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        }
       
    }

    //this method will check the tally vector and get the winner of the vote if they have majority + 1 votes from all aceptors.
    public String AnnounceWinner(Vector<Integer> tally)
    {
        int count = 1, tempCount;
        int nominee = tally.get(0);
        int temp = 0;
        for (int i = 0; i < (tally.size() - 1); i++) //i pointer, start at index 0 and go till size -1 (since j pointer will check index at size())
        {
            temp = tally.get(i); //store the int currently being checked
            tempCount = 0; //initilize a tmpcount
            for (int j = 1; j < tally.size(); j++) //j point goes from index 1 till end of vector
            {
            if (temp == tally.get(j)) //everytime int at i appears, increment tmpcount
                tempCount++;
            }
            if (tempCount > count) //if tmpcount is bigger than current true count
            {
            nominee = temp; //nominee ID is temp
            count += tempCount; //add temp count to count (since j incrementing every instance of the int after i, so must add to count not just replace count with tempcount so that "i" is included in count)
            }
        }
        if(nominee > 0 && count >= (ProcessPool.NUMBER_OF_ACCEPTORS/2 + 1))
        {
            return "The new president is council member " + nominee + " with " + count + " votes!"; 
        }
        else
        {
            return "vote failed";
        }
        
    }

    //may not need to be a static class
    //class that will have threads created to handle each acceptors message
    public static class ConnectionHandlerLearner extends Thread
    {
        public ObjectInputStream input;
        public ObjectOutputStream output;
        public Socket learnerSocket;

        public ConnectionHandlerLearner(Socket socket) throws SocketException
        {
            this.learnerSocket = socket;
        }

        public void run()
        {
            try
            {
                output = new ObjectOutputStream(learnerSocket.getOutputStream());
                input = new ObjectInputStream(learnerSocket.getInputStream());
                
                Message vote = (Message) input.readObject();
                //learner.tally.getAndSet(); set/increment each time a councillor votes for proposed
                tallyVector.add(vote.nominee);
                System.out.println("SENT BY proposerID: " + vote.proposer_ID);
                //System.out.println("adding to vector"); //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    
                    
            }
            catch(Exception e)
            {
                if(e instanceof java.io.EOFException)
                {
                }
                else
                {
                    System.err.println(e);
                    e.printStackTrace();
                }
            }
            finally
            {
                try 
                {
                    learnerSocket.close();
                } 
                catch (IOException e) 
                {
                    e.printStackTrace();
                }
            }

        }

    }
}
