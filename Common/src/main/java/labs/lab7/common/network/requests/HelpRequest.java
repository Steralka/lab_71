package labs.lab7.common.network.requests;

import labs.lab7.common.models.User;
import labs.lab7.common.utility.CommandType;

public class HelpRequest extends Request {

    public HelpRequest(User user) {
        super(CommandType.HELP.name(), user);
    }
}
