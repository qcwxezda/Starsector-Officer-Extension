package officerextension.filter;

import com.fs.starfarer.api.characters.OfficerDataAPI;
import officerextension.Util;

public class TagFilter implements OfficerFilter {

    private final String tagName;

    public TagFilter(String tagName) {
        this.tagName = tagName;
    }

    @Override
    public boolean check(OfficerDataAPI officer) {
        return Util.getOfficerTags(officer).contains(tagName);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TagFilter)) {
            return false;
        }
        return ((TagFilter) o).tagName.equals(tagName);
    }

    @Override
    public int hashCode() {
        return tagName.hashCode();
    }
}
