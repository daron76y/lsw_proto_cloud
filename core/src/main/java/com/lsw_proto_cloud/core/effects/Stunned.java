package com.lsw_proto_cloud.core.effects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Stunned extends Effect {
    @JsonCreator
    public Stunned(@JsonProperty("duration") int duration) {
        super(duration);
    }

    @Override
    public String getName() {return "Stunned";}

    @Override
    public boolean preventsAction() {return true;}
}
