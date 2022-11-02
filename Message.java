import java.io.*;
//the message object passed from proposer to acceptor to learner
public class Message implements Serializable
{
    String name = "";
    float proposer_ID = 0;
    float accepted_ID = 0;
    int nominee;
    String response = "";
    int port = 0;

    public Message()
    {
    }

    public Message(String response)
    {
        this.response = response;
    }

    public Message(String response, int nominee)
    {
        this.response = response;
        this.nominee = nominee;
    }

    public Message(float proposer_ID, int nominee, float accepted_ID)
    {
        this.proposer_ID = proposer_ID;
        this.nominee = nominee;
        this.accepted_ID = accepted_ID;
    }
    
    public Message(float proposer_ID)
    {
        this.proposer_ID = proposer_ID;
    }

    public Message(float proposer_ID, int nominee, String name)
    {
        this.proposer_ID = proposer_ID;
        this.nominee = nominee;
        this.name = name;
    }

    public Message(float proposer_ID, int nominee)
    {
        this.proposer_ID = proposer_ID;
        this.nominee = nominee;
    }
}
