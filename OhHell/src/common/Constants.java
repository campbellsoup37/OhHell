package common;

public class Constants {
    public static final String urlRegex = "(http(s)?:\\/\\/.)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)";
    //public static final String urlRegex = "((http:\\/\\/|https:\\/\\/)?(www.)?(([a-zA-Z0-9-]){2,}\\.){1,4}([a-zA-Z]){2,6}(\\/([a-zA-Z-_\\/\\.0-9#:?=&;,]*)?)?)";
    public static final String[][] htmlReservedChars = 
               {{"&", "&amp;"}, 
                {"<", "&lt;"}, 
                {">", "&gt;"}, 
                {"\"", "&quot;"}, 
                {"'", "&#39;"}};
    
    public static final int maxPlayersWithRobots = 10;
}
