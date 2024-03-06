package officerextension.filter;

import com.fs.starfarer.api.characters.OfficerDataAPI;

public interface OfficerFilter {
    boolean check(OfficerDataAPI officer);
}
