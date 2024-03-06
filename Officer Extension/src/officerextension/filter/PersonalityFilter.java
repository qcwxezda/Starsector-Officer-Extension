package officerextension.filter;

import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonalityAPI;

public class PersonalityFilter implements OfficerFilter {

    private final String id;
    private final String displayName;

    public PersonalityFilter(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean check(OfficerDataAPI officer) {
        return id.equals(officer.getPerson().getPersonalityAPI().getId());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PersonalityFilter)) return false;
        return id.equals(((PersonalityFilter) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
