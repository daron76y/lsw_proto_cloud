package com.lsw_proto_cloud.core.effects;

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
        @JsonSubTypes.Type(value = FireShield.class, name = "FireShield"),
        @JsonSubTypes.Type(value = ManaBurn.class, name = "ManaBurn"),
        @JsonSubTypes.Type(value = Shield.class, name = "Shield"),
        @JsonSubTypes.Type(value = SneakAttack.class, name = "SneakAttack"),
        @JsonSubTypes.Type(value = Stunned.class, name = "Stunned")
})

public abstract class Effect {
    private int duration; //how many turns

    @JsonCreator
    public Effect(@JsonProperty("duration") int duration) {
        this.duration = duration;
    }

    @JsonIgnore public abstract String getName();
    public int getDuration() {return duration;}
    public void decrementDuration() {duration--;}
    @JsonIgnore public boolean isExpired() {return duration <= 0;}

    // optional overrides unique to each effect. Not all effects need to override/implement
    public void onAttack(Unit attacker, Party allyParty, Unit target, Party enemyParty, OutputService output) {}
    public int modifyDamage(Unit attacker, Unit target, int damage, OutputService output) {return damage;}
    public boolean preventsAction() {return false;}
}
