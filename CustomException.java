//a customexception so that I can have more freedom in creating conditions based off of
//more than just the SocketTimeout exception.
public class CustomException extends Exception
{   
    public CustomException(String errorMessage)
    {
        super(errorMessage);
    }
}
