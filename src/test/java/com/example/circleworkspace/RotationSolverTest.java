package com.example.circleworkspace;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.circleworkspace.Model.*;
class RotationSolverTest {
 @Test void chainRecomputesFromAuthoritativeSlaveIds(){
  var circles=List.of(new CircleState(1,0,0,100,0,true,6,null),new CircleState(2,150,0,50,0,false,0,1),new CircleState(3,250,0,50,0,false,0,2));
  var contacts=List.of(new ContactState(1,1,2,Tangency.EXTERNAL,90,270),new ContactState(2,2,3,Tangency.EXTERNAL,90,270));
  var r=new RotationSolver().solve(circles,contacts);
  assertEquals(-12,r.get(2).rateDegPerTick(),1e-9); assertEquals(12,r.get(3).rateDegPerTick(),1e-9);
 }
}
