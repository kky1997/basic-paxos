import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

//address book used by acceptors to keep add their ports so proposers know how to contact them
class AddressBook 
{
    volatile public static Vector<Integer> AddressBookLookUp = new Vector<Integer>();


    public AddressBook()
    {
    }
}
//extends thread, so can create a thread for each member
public class Member extends Thread
{
    private String name = "";
    private boolean is_President = false;
    private boolean desires_President = false;
    private boolean proposer = false;
    private boolean learner = false;
    private boolean acceptor = false;
    private int response_Time = 0;


    public Member(String name, boolean status, boolean desire, boolean proposer, boolean learner, boolean acceptor, int rt)
    {
        this.name = name;
        this.is_President = status;
        this.desires_President = desire;
        this.proposer = proposer;
        this.learner = learner;
        this.acceptor = acceptor;
        
        if(rt > 3 || rt < 0)
        {
            System.out.println("please enter another response time");
        }
        else
        {
            this.response_Time = rt;
        }
    }

    public Member(String name, int rt)
    {
        this.name = name;     
        if(rt > 3 || rt < 0)
        {
            System.out.println("please enter another response time");
        }
        else
        {
            this.response_Time = rt;
        }
    }

    public Member(String name)
    {
        this.name = name;
    }

    public int splitName(String name)
    {
        String[] characters = name.split("(?<=\\D)(?=\\d)");
        return Integer.parseInt(characters[1]);
    }

    public String get_Name()
    {
        return this.name;
    }

    public void set_Name(String name)
    {
        this.name = name;
    }

    public boolean get_IsPresident()
    {
        return this.is_President;
    }

    public void set_IsPresident(boolean status)
    {
        this.is_President = status;
    }

    public boolean get_Desire()
    {
        return this.desires_President;
    }

    public void set_Desire(boolean desire)
    {
        this.desires_President = desire;
    }

    public boolean get_Proposer()
    {
        return this.proposer;
    }

    public void set_Proposer(boolean proposer)
    {
        this.proposer = proposer;
    }

    public boolean get_Leaner()
    {
        return this.learner;
    }

    public void set_Leaner(boolean learner)
    {
        this.learner = learner;
    }

    public boolean get_Acceptor()
    {
        return this.acceptor;
    }

    public void set_Acceptor(boolean acceptor)
    {
        this.acceptor = acceptor;
    }

    public int get_ResponseTime()
    {
        return this.response_Time;
    }

    public void set_ResponseTime(int rt)
    {
        this.response_Time = rt;
    }
}

