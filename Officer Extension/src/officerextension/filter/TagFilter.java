package officerextension.filter;

import com.fs.starfarer.api.characters.OfficerDataAPI;

public abstract class TagFilter implements OfficerFilter {

    protected String tagName;

    @Override
    public abstract boolean check(OfficerDataAPI officer);

    /** InnateTagFilters and CustomTagFilters should not be equal. */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        return ((TagFilter) o).tagName.equals(tagName);
    }

    /** InnateTagFilters and CustomTagFilters should not be equal. */
    @Override
    public int hashCode() {
        return tagName.hashCode() + getClass().hashCode();
    }
}

