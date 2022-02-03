package officerextension.filter;

import com.fs.starfarer.api.characters.OfficerDataAPI;
import officerextension.Util;

public class CustomTagFilter extends TagFilter {

    public CustomTagFilter(String tagName) {
        this.tagName = tagName;
    }

    @Override
    public boolean check(OfficerDataAPI officer) {
        return Util.getOfficerTags(officer).contains(tagName);
    }
}
