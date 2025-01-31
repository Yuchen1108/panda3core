// PANDA 3 -- a domain-independent planner for classical and hierarchical planning
// Copyright (C) 2014-2018 the original author or authors.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package de.uniulm.ki.panda3.problemGenerators.cfgIntersectionGenerator;

import de.uniulm.ki.panda3.symbolic.csp.CSP;
import de.uniulm.ki.panda3.symbolic.csp.VariableConstraint;
import de.uniulm.ki.panda3.symbolic.domain.*;
import de.uniulm.ki.panda3.symbolic.ioInterface.FileHandler;
import de.uniulm.ki.panda3.symbolic.logic.*;
import de.uniulm.ki.panda3.symbolic.plan.Plan;
import de.uniulm.ki.panda3.symbolic.plan.element.CausalLink;
import de.uniulm.ki.panda3.symbolic.plan.element.OrderingConstraint;
import de.uniulm.ki.panda3.symbolic.plan.element.PlanStep;
import de.uniulm.ki.panda3.symbolic.plan.flaw.AbstractPlanStep;
import de.uniulm.ki.panda3.symbolic.plan.flaw.CausalThreat;
import de.uniulm.ki.panda3.symbolic.plan.flaw.OpenPrecondition;
import de.uniulm.ki.panda3.symbolic.plan.flaw.UnboundVariable;
import de.uniulm.ki.panda3.symbolic.plan.modification.*;
import de.uniulm.ki.panda3.symbolic.plan.ordering.TaskOrdering;
import de.uniulm.ki.panda3.symbolic.sat.additionalConstraints.LTLTrue;
import de.uniulm.ki.panda3.symbolic.sat.additionalConstraints.LTLTrue$;
import de.uniulm.ki.panda3.symbolic.search.FlawsByClass;
import de.uniulm.ki.panda3.symbolic.search.IsFlawAllowed;
import de.uniulm.ki.panda3.symbolic.search.IsModificationAllowed;
import de.uniulm.ki.panda3.symbolic.search.ModificationsByClass;
import de.uniulm.ki.panda3.util.JavaToScala;
import de.uniulm.ki.panda3.util.seqProviderList;
import scala.None$;
import scala.Tuple2;
import scala.collection.Seq;
import scala.collection.immutable.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dh on 20.02.17.
 */
public class CfGrammarIntersectionToHTN {

    private final static List<Class<?>> alwaysAllowedModificationsClasses = new LinkedList<>();
    private final static List<Class<?>> alwaysAllowedFlawClasses = new LinkedList<>();

    static {

        alwaysAllowedModificationsClasses.add(AddOrdering.class);
        alwaysAllowedModificationsClasses.add(BindVariableToValue.class);
        alwaysAllowedModificationsClasses.add(InsertCausalLink.class);
        alwaysAllowedModificationsClasses.add(MakeLiteralsUnUnifiable.class);

        alwaysAllowedFlawClasses.add(CausalThreat.class);
        alwaysAllowedFlawClasses.add(OpenPrecondition.class);
        alwaysAllowedFlawClasses.add(UnboundVariable.class);
    }

    public final static scala.collection.immutable.Map<PlanStep, DecompositionMethod> planStepsDecomposedBy =
            scala.collection.immutable.Map$.MODULE$.<PlanStep, DecompositionMethod>empty();
    public final static scala.collection.immutable.Map<PlanStep, Tuple2<PlanStep, PlanStep>> planStepsDecompositionParents =
            scala.collection.immutable.Map$.MODULE$.<PlanStep, Tuple2<PlanStep, PlanStep>>empty();

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Please provide the two grammars to be intersected and two files for the output (that will be overwritten!)." +
                    "\n program gr1.txt gr2.txt out-domain.txt out-problem.txt");
            return;
        }
        String g1_path = args[0];
        String g2_path = args[1];

        CfGrammar g1 = new CfGrammar(g1_path);
        CfGrammar g2 = new CfGrammar(g2_path);

        Tuple2<Domain, Plan> prob = grammerInterProb(g1, g2);
        FileHandler.writeHDDLToFiles(prob, args[2], args[3]);
    }

    static String sG1 = "G1";
    static String sG2 = "G2";

    static int id = 0;
    static Task epsilon = new ReducedTask("epsilon", true, new Vector<Variable>(0, 0, 0), new Vector<Variable>(0, 0, 0), new Vector<VariableConstraint>(0, 0, 0), new And<>(new
            Vector<Literal>(0, 0, 0)), new And<>(new Vector<Literal>(0, 0, 0)),
            new ConstantActionCost(1));

    public static Tuple2<Domain, Plan> grammerInterProb(CfGrammar g1, CfGrammar g2) {


        List<Class<?>> allowedModificationsClasses = new LinkedList<Class<?>>();
        List<Class<?>> allowedFlawClasses = new LinkedList<Class<?>>();
        allowedModificationsClasses.addAll(alwaysAllowedModificationsClasses);
        allowedFlawClasses.addAll(alwaysAllowedFlawClasses);
        allowedModificationsClasses.add(DecomposePlanStep.class);
        allowedFlawClasses.add(AbstractPlanStep.class);

        IsModificationAllowed allowedModifications = new ModificationsByClass(JavaToScala.toScalaSeq(allowedModificationsClasses));
        IsFlawAllowed allowedFlaws = new FlawsByClass(JavaToScala.toScalaSeq(allowedFlawClasses));

        Seq<Sort> sorts = (new seqProviderList<Sort>()).result();
        seqProviderList<Predicate> predicates = new seqProviderList<Predicate>();

        seqProviderList<Task> tasks = new seqProviderList<Task>();
        tasks.add(epsilon);

        Predicate turnA = new Predicate("turnA", new Vector<>(0, 0, 0));
        Literal turnAPos = new Literal(turnA, true, new Vector<>(0, 0, 0));
        Literal turnANeg = new Literal(turnA, false, new Vector<>(0, 0, 0));

        Predicate turnB = new Predicate("turnB", new Vector<>(0, 0, 0));
        Literal turnBPos = new Literal(turnB, true, new Vector<>(0, 0, 0));
        Literal turnBNeg = new Literal(turnB, false, new Vector<>(0, 0, 0));

        predicates.add(turnA);
        predicates.add(turnB);

        HashMap<String, Task> tasksMap = new HashMap<>();
        HashMap<String, Predicate> predMap = new HashMap<>();
        for (String tname : g1.terminal) {
            Predicate tPred = new Predicate("l" + tname, new Vector<>(0, 0, 0));
            predMap.put(tPred.name(), tPred);
            predicates.add(tPred);

            seqProviderList<Literal> precLits = new seqProviderList<>();
            precLits.add(turnAPos);
            And<Literal> prec = new And<Literal>(precLits.result());

            seqProviderList<Literal> effLits = new seqProviderList<>();
            effLits.add(turnANeg);
            effLits.add(turnBPos);
            effLits.add(new Literal(tPred, true, new Vector<Variable>(0, 0, 0)));
            And<Literal> eff = new And<Literal>(effLits.result());

            Task t = new ReducedTask(tname + sG1, true, new Vector<Variable>(0, 0, 0), new Vector<Variable>(0, 0, 0), new Vector<VariableConstraint>(0, 0, 0), prec, eff,
                    new ConstantActionCost(1));
            tasksMap.put(tname + sG1, t);
            tasks.add(t);
        }

        for (String tname : g2.terminal) {
            Predicate tPred = predMap.get("l" + tname);

            seqProviderList<Literal> precLits = new seqProviderList<>();
            precLits.add(turnBPos);
            precLits.add(new Literal(tPred, true, new Vector<Variable>(0, 0, 0)));
            And<Literal> prec = new And<Literal>(precLits.result());

            seqProviderList<Literal> effLits = new seqProviderList<>();
            effLits.add(turnBNeg);
            effLits.add(turnAPos);
            effLits.add(new Literal(tPred, false, new Vector<Variable>(0, 0, 0)));
            And<Literal> eff = new And<Literal>(effLits.result());

            Task t = new ReducedTask(tname + sG2, true, new Vector<Variable>(0, 0, 0), new Vector<Variable>(0, 0, 0), new Vector<VariableConstraint>(0, 0, 0), prec, eff,
                    new ConstantActionCost(1));
            tasksMap.put(tname + sG2, t);
            tasks.add(t);
        }

        for (String tname : g1.nonterminal) {
            Task t = new ReducedTask(tname + sG1, false, new Vector<Variable>(0, 0, 0), new Vector<Variable>(0, 0, 0), new Vector<VariableConstraint>(0, 0, 0), new And<>(new
                    Vector<>(0, 0, 0)), new And<>(new Vector<>(0, 0, 0)), new ConstantActionCost(0));
            tasksMap.put(tname + sG1, t);
            tasks.add(t);
        }

        for (String tname : g2.nonterminal) {
            Task t = new ReducedTask(tname + sG2, false, new Vector<Variable>(0, 0, 0), new Vector<Variable>(0, 0, 0), new Vector<VariableConstraint>(0, 0, 0), new And<>(new
                    Vector<>(0, 0, 0)), new And<>(new Vector<>(0, 0, 0)), new ConstantActionCost(0));
            tasksMap.put(tname + sG2, t);
            tasks.add(t);
        }

        seqProviderList<DecompositionMethod> decompositionMethods = new seqProviderList<DecompositionMethod>();
        addMethods(g1, allowedModifications, allowedFlaws, tasksMap, sG1, decompositionMethods);
        addMethods(g2, allowedModifications, allowedFlaws, tasksMap, sG2, decompositionMethods);

        Seq<DecompositionAxiom> decompositionAxioms = new Vector<>(0, 0, 0);
        Domain d = new Domain(sorts, predicates.result(), tasks.result(), decompositionMethods.result(), decompositionAxioms,
                JavaToScala.toScalaMap(new HashMap<GroundLiteral, Object>()),
                None$.empty(), None$.empty());

        seqProviderList<Literal> s0Lits = new seqProviderList<>();
        s0Lits.add(turnAPos);
        And<Literal> g1Turn = new And<Literal>(s0Lits.result());

        ReducedTask initSchema = new ReducedTask("init", true, new Vector<>(0, 0, 0), new Vector<>(0, 0, 0), new Vector<VariableConstraint>(0, 0, 0), new And<Literal>(new
                Vector<Literal>(0, 0, 0)), g1Turn,
                new ConstantActionCost(0));
        ReducedTask goalSchema = new ReducedTask("goal", true, new Vector<>(0, 0, 0), new Vector<>(0, 0, 0), new Vector<VariableConstraint>(0, 0, 0), g1Turn, new And<Literal>
                (new Vector<Literal>(0, 0, 0)), new ConstantActionCost(0));
        PlanStep psInit = new PlanStep(-1, initSchema, new Vector<>(0, 0, 0));
        PlanStep psGoal = new PlanStep(-2, goalSchema, new Vector<>(0, 0, 0));

        CSP csp = new CSP(JavaToScala.toScalaSet(new ArrayList<Variable>()), new seqProviderList<VariableConstraint>().result());
        seqProviderList<PlanStep> planSteps = new seqProviderList<>();
        planSteps.add(psInit);
        planSteps.add(psGoal);

        seqProviderList<OrderingConstraint> ordSeq = new seqProviderList<>();

        Task g1Start = tasksMap.get(g1.start + sG1);
        PlanStep ps1Start = new PlanStep(0, g1Start, new Vector<>(0, 0, 0));
        planSteps.add(ps1Start);
        Task g2Start = tasksMap.get(g2.start + sG2);
        PlanStep ps2Start = new PlanStep(1, g2Start, new Vector<>(0, 0, 0));
        planSteps.add(ps2Start);

        ordSeq.add(new OrderingConstraint(psInit, psGoal));
        ordSeq.add(new OrderingConstraint(psInit, ps1Start));
        ordSeq.add(new OrderingConstraint(psInit, ps2Start));
        ordSeq.add(new OrderingConstraint(ps1Start, psGoal));
        ordSeq.add(new OrderingConstraint(ps2Start, psGoal));

        TaskOrdering taskOrderings = new TaskOrdering(ordSeq.result(), planSteps.result());
        Plan p = new Plan(planSteps.result(), new seqProviderList<CausalLink>().result(), taskOrderings, csp, psInit, psGoal,
                allowedModifications, allowedFlaws, planStepsDecomposedBy, planStepsDecompositionParents, false, LTLTrue$.MODULE$);

        return new Tuple2<>(d, p);
    }

    private static void addMethods(CfGrammar gr, IsModificationAllowed allowedModifications, IsFlawAllowed allowedFlaws, HashMap<String, Task> tasksMap, String grammarStr, seqProviderList<DecompositionMethod> decompositionMethods) {
        for (int j = 0; j < gr.rulesLeft.size(); j++) {
            String name = grammarStr + "-" + gr.rulesLeft.get(j) + "2";

            Task absT = tasksMap.get(gr.rulesLeft.get(j) + grammarStr);
            seqProviderList<PlanStep> subtasks = new seqProviderList<>();
            boolean first = true;
            for (String subTaskStr : gr.rulesRight.get(j)) {
                if (first) {
                    first = false;
                } else
                    name += "-";
                name += subTaskStr;
                Task subT = tasksMap.get(subTaskStr + grammarStr);
                PlanStep ps = new PlanStep(id++, subT, new Vector<Variable>(0, 0, 0));
                subtasks.add(ps);
            }
            if (subtasks.size() == 0) {
                PlanStep ps = new PlanStep(id++, epsilon, new Vector<Variable>(0, 0, 0));
                subtasks.add(ps);
            }
            seqProviderList<OrderingConstraint> ordSeq = new seqProviderList<>();
            for (int i = 1; i < subtasks.size(); i++) {
                ordSeq.add(new OrderingConstraint(subtasks.get(i - 1), subtasks.get(i)));
            }
            CSP csp = new CSP(JavaToScala.toScalaSet(new ArrayList<Variable>()), new seqProviderList<VariableConstraint>().result());

            GeneralTask initSchema = new GeneralTask("init", true, absT.parameters(), absT.parameters(), new Vector<VariableConstraint>(0, 0, 0),
                    new And<Literal>(new Vector<Literal>(0, 0, 0)), new And<Literal>(new Vector<Literal>(0, 0, 0)), new ConstantActionCost(0));
            GeneralTask goalSchema = new GeneralTask("goal", true, absT.parameters(), absT.parameters(), new Vector<VariableConstraint>(0, 0, 0),
                    new And<Literal>(new Vector<Literal>(0, 0, 0)), new And<Literal>(new Vector<Literal>(0, 0, 0)), new ConstantActionCost(0));
            PlanStep psInit = new PlanStep(-1, initSchema, absT.parameters());
            PlanStep psGoal = new PlanStep(-2, goalSchema, absT.parameters());
            subtasks.add(psInit);
            subtasks.add(psGoal);
            for (int i = 0; i < subtasks.size(); i++) {
                ordSeq.add(new OrderingConstraint(psInit, subtasks.get(i)));
                ordSeq.add(new OrderingConstraint(subtasks.get(i), psGoal));
            }
            ordSeq.add(new OrderingConstraint(psInit, psGoal));

            TaskOrdering ordering = new TaskOrdering(ordSeq.result(), subtasks.result());
            Plan subplan = new Plan(subtasks.result(), new Vector<CausalLink>(0, 0, 0), ordering, csp, psInit, psGoal, allowedModifications, allowedFlaws, planStepsDecomposedBy,
                    planStepsDecompositionParents, false, LTLTrue$.MODULE$);
            DecompositionMethod dm = new SimpleDecompositionMethod(absT, subplan, name);
            decompositionMethods.add(dm);
        }
    }
}
