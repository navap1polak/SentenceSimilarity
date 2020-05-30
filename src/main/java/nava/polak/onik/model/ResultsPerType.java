package nava.polak.onik.model;

import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class ResultsPerType {

    private Set<Integer> recordsIndex = new HashSet<>();
    private PatternType type;

    public ResultsPerType(PatternType type) {
        this.type = type;
    }

    public void addRecord(int index){
        recordsIndex.add(index);
    }

    public String getRecordsStr(List<Record> sentences){
        StringBuilder recordsStr = new StringBuilder();
        StringBuilder changedParamStr = new StringBuilder("The changing word was: ");

        recordsIndex.stream().forEach(index->{
            Record record = sentences.get(index);
            recordsStr.append(record.getFullRecord());
            recordsStr.append("\n");
            String word_sep = " , ";
            changedParamStr.append(record.getWordAccorfingType(type)).append(word_sep);

        });

        recordsStr.append(changedParamStr.substring(0,changedParamStr.length()-3));
        recordsStr.append("\n");

        return recordsStr.toString();
    }
}
