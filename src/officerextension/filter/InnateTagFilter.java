package officerextension.filter;

import com.fs.starfarer.api.characters.OfficerDataAPI;

public class InnateTagFilter extends TagFilter {

    public InnateTagFilter(String tagName) {
        this.tagName = tagName;
    }

    @Override
    public boolean check(OfficerDataAPI officer) {
        return officer.getPerson().hasTag(tagName) || officer.getPerson().getMemoryWithoutUpdate().getBoolean(tagName);
    }
}
