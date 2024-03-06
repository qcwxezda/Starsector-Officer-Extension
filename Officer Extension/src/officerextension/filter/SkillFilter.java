package officerextension.filter;

import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;

public class SkillFilter implements OfficerFilter {

    private final SkillSpecAPI skill;
    private boolean isElite = false;

    public SkillFilter(SkillSpecAPI skill) {
        this.skill = skill;
    }

    public boolean isElite() {
        return isElite;
    }

    public void setElite(boolean value) {
        isElite = value;
    }

    @Override
    public boolean check(OfficerDataAPI officer) {
        return officer.getPerson().getStats().getSkillLevel(skill.getId()) > (isElite ? 1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SkillFilter)) {
            return false;
        }
        return ((SkillFilter) o).skill.getId().equals(skill.getId());
    }

    @Override
    public int hashCode() {
        return skill.getId().hashCode();
    }
}
