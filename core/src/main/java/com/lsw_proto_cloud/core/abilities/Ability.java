package com.lsw_proto_cloud.core.abilities;

import com.fasterxml.jackson.annotation.*;
import com.lsw_proto_cloud.core.OutputService;
import com.lsw_proto_cloud.core.Party;
import com.lsw_proto_cloud.core.Unit;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BerserkerAttack.class, name = "BerserkerAttack"),
        @JsonSubTypes.Type(value = ChainLightning.class, name = "ChainLightning"),
        @JsonSubTypes.Type(value = Fireball.class, name = "Fireball"),
        @JsonSubTypes.Type(value = Heal.class, name = "Heal"),
        @JsonSubTypes.Type(value = Protect.class, name = "Protect"),
        @JsonSubTypes.Type(value = Replenish.class, name = "Replenish")
})

public abstract class Ability {
    int manaCost;

    @JsonCreator
    public Ability(@JsonProperty("manaCost") int manaCost) {this.manaCost = manaCost;}
    @JsonIgnore public abstract String getName();
    public int getManaCost() {return this.manaCost;}
    public abstract boolean requiresTarget();

    //template method because all abilities share the same basic cast output
    public void execute(Unit caster, Unit target, Party allyParty, Party enemyParty, OutputService output) {
        output.showMessage(String.format("%s casts %s!", caster.getName(), getName()));
        perform(caster, target, allyParty, enemyParty, output);
    }

    //unique ability implementations
    public abstract void perform(Unit caster, Unit target, Party allyParty, Party enemyParty, OutputService output);
}
