package nava.polak.onik.model;

import lombok.Data;

@Data
public class Record {
    private String log;
    private String action;
    private String name;
    private String restStr;

    public Record(String log, String name, String action , String restStr) {
        this.log = log;
        this.action = action;
        this.name = name;
        this.restStr = restStr;
    }

    public String getWordAccorfingType(PatternType type){
        switch(type){
            case NAME:
                return name;
            case ACTION:
                return action;
            default:
                return restStr;
        }
    }

    public String constructKeyAccordingType(PatternType type) {
        switch (type) {
            case NAME:
                return action + "_" + restStr;
            case ACTION:
                return name + restStr;
            case REST:
                return name + "_" + action;
        }
        return null;

    }

    public String getFullRecord(){
        return log + " " + name + " is " + action + " " + restStr;
    }
}
