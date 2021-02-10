package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import sun.tools.jconsole.LabeledComponent;

public class CodeGenerator extends VisitorAdaptor {
	
	private int mainPc;
	private LinkedList<ArrayList<Integer>> breakStack = new LinkedList<ArrayList<Integer>>(); //pamte se break-ovi
	private LinkedList<ArrayList<Integer>> continueStack = new LinkedList<ArrayList<Integer>>(); //pamte se continue-ovi
	
	public CodeGenerator() {
		// 'ord' i 'chr' imaju isti kod
        Obj ordMethod = Tab.find("ord");
        Obj chrMethod = Tab.find("chr");
        ordMethod.setAdr(Code.pc);
        chrMethod.setAdr(Code.pc);
        Code.put(Code.enter);
        Code.put(1);
        Code.put(1);
        Code.put(Code.load_n);
        Code.put(Code.exit);
        Code.put(Code.return_);
 
        Obj lenMethod = Tab.find("len");
        lenMethod.setAdr(Code.pc);
        Code.put(Code.enter);
        Code.put(1);
        Code.put(1);
        Code.put(Code.load_n);
        Code.put(Code.arraylength);
        Code.put(Code.exit);
        Code.put(Code.return_);
	}
	
	public int getMainPc() {
		return mainPc;
	}
	
	
	public void visit(MthRetTypeAndName mthRetTypeAndName) {
		mthRetTypeAndName.obj.setAdr(Code.pc);
		
		if(mthRetTypeAndName.obj.getName().equals("main"))
			mainPc = Code.pc;
		
		Code.put(Code.enter);
		int fPars = 0;
		for(Obj par: mthRetTypeAndName.obj.getLocalSymbols())
			if(par.getFpPos() == 1)
				fPars++;
		Code.put(fPars);
		Code.put(mthRetTypeAndName.obj.getLocalSymbols().size());
	}
	
	public void visit(MethodDeclaration methodDeclaration) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	
	public void visit(FactorNumConst factorNumConst) {
		Code.loadConst(factorNumConst.getValue());
		if(minusHappend.peek() == true) {
			Code.put(Code.neg);
			minusHappend.pop();
			minusHappend.push(false);
		}
	}
	
	public void visit(FactorCharConst factorCharConst) {
		Code.loadConst(factorCharConst.getValue());
	}
	
	public void visit(FactorBoolConst factorBoolConst) {
		Code.loadConst(factorBoolConst.getValue());
	}
	
	public void visit(FactorSingleDesignator factorSingleDesignator) {
		Code.load(factorSingleDesignator.getDesignator().obj);
		if(minusHappend.peek() == true) {
			Code.put(Code.neg);
			minusHappend.pop();
			minusHappend.push(false);
		}
	}
	
	public void visit(FactorNewTypeExpr factorNewTypeExpr) {
		Code.put(Code.newarray);
		if(factorNewTypeExpr.getType().struct == Tab.charType)
			Code.put(0);
		else
			Code.put(1);
	}
	
	public void visit(FactorExpr factorExpr) {
		if(minusHappend.peek() == true) {
			Code.put(Code.neg);
			minusHappend.pop();
			minusHappend.push(false);
		}
	}
	
	public void visit(TermAddop termAddop) {
		if(termAddop.getAddop() instanceof AddopPlus)
			Code.put(Code.add);
		else
			Code.put(Code.sub);
	}
	
	public void visit(DesignatorStmAssignop designatorStmAssignop) { //storuje vrednost sa expr steka u designator
		Code.store(designatorStmAssignop.getDesignator().obj);
	}
	
	public void visit(FactorListMulop factorListMulop) {
		if(factorListMulop.getMulop() instanceof MulopMul) {
			Code.put(Code.mul);
		}
		if(factorListMulop.getMulop() instanceof MulopDiv) {
			Code.put(Code.div);
		}
		if(factorListMulop.getMulop() instanceof MulopMod) {
			Code.put(Code.rem);
		}
		
	}
	
	public void visit(ExprSingleMinusTerm exprSingleMinusTerm) {
		minusHappend.pop();
	}
	
	
	public void visit(StatementNoPrintNumconst statementNoPrintNumconst) {
		Code.loadConst(1);
		if(statementNoPrintNumconst.getExpr().struct.equals(Tab.intType)|| statementNoPrintNumconst.getExpr().struct.equals(SemanticPass.boolType)){
			Code.put(Code.print);
		}
		else if(statementNoPrintNumconst.getExpr().struct.equals(Tab.charType)) {
			Code.put(Code.bprint);
		}
	}
	public void visit(StatementPrint statementPrint) {
		Code.loadConst(statementPrint.getN2());
		if(statementPrint.getExpr().struct.equals(Tab.intType)|| statementPrint.getExpr().struct.equals(SemanticPass.boolType)){
			Code.put(Code.print);
		}
		else if(statementPrint.getExpr().struct.equals(Tab.charType)) {
			Code.put(Code.bprint);
		}
	}
	
	public void visit(StatementRead statementRead) {
		if(statementRead.getDesignator().obj.getType().equals(Tab.charType)) {
			Code.put(Code.bread);
		}
		else
			Code.put(Code.read);
		Code.store(statementRead.getDesignator().obj);  //storuje sta je procitao
	}
	
	public void visit(StatementNoReturnExpr statementNoReturnExpr) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(StatementReturnExpr statementReturnExpr) {
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(DesignatorStmInc designatorStmInc) {
		Code.load(designatorStmInc.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(designatorStmInc.getDesignator().obj);
	}
	
	public void visit(DesignatorStmDec designatorStmDec) {
		Code.load(designatorStmDec.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(designatorStmDec.getDesignator().obj);
	}
	
	public void visit(DesignatorArrName designatorArrName) {
		Code.load(designatorArrName.obj);
	}
	
	Stack<Boolean> minusHappend = new Stack<Boolean>();
	public void visit(YesMinus YesMinus) {
		minusHappend.push(true);
	}
	
	public void visit(NoMinus noMinus) {
		minusHappend.push(false);
	}
	
	public void visit(FactorDesignatorActPars factorDesignatorActPars) {
		int offset = factorDesignatorActPars.getDesignator().obj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
	}
	
	public void visit(DesignatorStmActPars designatorStmActPars) {
		int offset = designatorStmActPars.getDesignator().obj.getAdr() - Code.pc;
		Code.put(Code.call);
		Code.put2(offset);
		
		if(designatorStmActPars.getDesignator().obj.getType() != Tab.noType) {
			Code.put(Code.pop);
		}
	}
	
	Stack<Integer> falseCF = new Stack<>();
	public void visit(CondFactMoreExpr condFactMoreExpr) {
		Code.putFalseJump(condFactMoreExpr.getRelop().struct.getKind(), 0);//ovde iskacu false
		falseCF.push(Code.pc - 2);
		//ovde nastavljaju true
	}
	
	public void visit(CondFactSingleExpr condFactSingleExpr) {
		Code.loadConst(0);
		Code.putFalseJump(Code.ne, 0);//ovde iskacu false
		falseCF.push(Code.pc - 2);
		//ovde nastavljaju true
	}
	
	Stack<Integer> trueOR = new Stack<>();
	public void visit(CondTerm condTerm) {
		//ovde su svi kojima su na svim && uslovima true
		Code.putJump(0);
		trueOR.push(Code.pc - 2);
		//ovde treba da doskoce svi koji su bili false pre ovoga
		while(!falseCF.empty()) {
			Code.fixup(falseCF.pop());
		}
		//ovde nastavljaju svi koji su bili false na nekom && i ispituju sledeci || uslov
	}
	
	Stack<Integer> skipTrue= new Stack<>();
	public void visit(Condition condition) { /* a > 3  && true  || a == 2 && c <= 2	 */
		//svi koji su doslocili na sledeci || uslov odnosno bil su false ali vise nema ||-ova
		Code.putJump(0);//bacam ih na else
		skipTrue.push(Code.pc - 2);
		while(!trueOR.empty()) {
			Code.fixup(trueOR.pop());
		}
		//ovde sam vratila sve koji su imali tacan  || uslov
		//ovo znaci da na kraju svakog Conditiona se nalaze svi koji su bili true a u skipTrue se nalazi skok koji baca sve koji su bili false i mora da se
		//fixupuje na pravo mesto
	}
	
	Stack<Integer> doStartAdr = new Stack<>();
	
	public void visit(DoTerm doTerm) {
		breakStack.add(new ArrayList<Integer>());
		continueStack.add(new ArrayList<Integer>());
		doStartAdr.push(Code.pc);
	}
	
	public void visit(StmWhile stmWhile) {

		ArrayList<Integer> continuePc = continueStack.removeLast();
		for (int i = 0; i < continuePc.size(); i++) {			
			Code.fixup(continuePc.get(i));			
		}
	}
	public void visit(StatementDoWhile statementDoWhile) {
									
		//ovde su svi koje je Condition ostavio da rade then (bili su true)
		Code.putJump(doStartAdr.pop());//bacila sam sve tacne na pocetak do
		//sad ovde vracam iz vazduha sve koji nisu ispunili uslov
		Code.fixup(skipTrue.pop());
		
		ArrayList<Integer> breakPc = breakStack.removeLast();
		for (int i = 0; i < breakPc.size(); i++) {			
			Code.fixup(breakPc.get(i));			
		}
	
	}
	
	//for(i = 0; i<3 ; i++) { ... }

	public void visit(StatementBreak stmBreak) {
		Code.put(Code.jmp);
		Code.put2(0);
		breakStack.getLast().add(Code.pc-2);
		
	}
	
	public void visit(StatementContinue stmCont) {
		Code.put(Code.jmp);
		Code.put2(0);
		continueStack.getLast().add(Code.pc-2);
		
	}
	//a>3 || 5 == 2 ? 1 : 2;
	Stack<Integer> skipElseTernary = new Stack<>();
	public void visit(Colon colon) {
		Code.put(Code.jmp);   //stavljam u vazduh one koji su prosli condition(true su bili)
		Code.put2(0);
		skipElseTernary.push(Code.pc - 2);
		Code.fixup(skipTrue.pop());  //ovde vracam one koji su bili false
	}
	public void visit(ExpressionTernary exprTer) {
		Code.fixup(skipElseTernary.pop());  //obidjen ceo ternary, vracam na kraj one koji su bili true(u vazduhu)
	}
	Stack<Integer> skipElse= new Stack<>();
	public void visit(Else elseIf) {
		Code.put(Code.jmp); //stavljam u vazduh one koji su prosli if uslov
		Code.put2(0);
		 skipElse.push(Code.pc-2);
		Code.fixup(skipTrue.pop()); //ovde vracam one koji nisu prosli
	}
	public void visit(NoELse noElse) {
		Code.fixup(skipTrue.pop()); //ovde vracam one koji nisu prosli if, ali nema elsa
	}                               //oni koji su prosli if su vec tu
	
	public void visit(YesElse yesElse) {  //ovo znaci da je else obidjen
		Code.fixup(skipElse.pop());//ovde vracam one koji su bili u vazduhu(prosli if)
		}
		                            
	
}


	   //WHILE
	//Stack<Integer> whileStartAddr= new Stack<>();
	//public void visit(While whileS) {
		//whileStartAddr.add(Code.pc);
	//}
	//public void visit(StmtWhile stmtWhile) {
		//Code.putJump(whileStartAddr.pop());
		//Code.fixup(skipTrue.pop());
	//}
	   //FOR
	//Stack<Integer> forAddr= new Stack<>();
	//public void visit(ConditionFor conditionFor) {
		//forAddr.push(Code.pc);
	//}
	//public void visit(StatementFor statementFor) {
		//Code.putJump(forAddr.pop());
		//Code.fixup(skipTrue.pop());
	//}
	
	  //GOTO
	//public void visit(LabelaStatement labela){   LabelaStatement ::= NazivLabela DDOTS  NazivLabela ::= IDENT:name
	// String lab= labela.getNazivLabela().getName();
	//if(labele.containsKey(lab)) {
		//Integer adr = labele.get(lab);
		//Code.fixup(adr};
     //}
	//else {
		//Integer adr=Code.pc;
		//labele.put(lab,adr);
	//}
	//public void visit(GoToStm stm) {     GoToStm ::= GOTO Labela SEMI    Labela ::= IDENT:name
		//String labela=stm.getLabela().getName();
		//if(labele.containsKey(labela)) {
			//Integer adr=labele.get(labela);
			//Code.putJump(adr);
		//}
		//
	//else {
		//Code.putJump(0);
		//labele.put(labela,Code.pc-2);
	//}
	//SUMA NIZA
	/*public void aaraysum(Obj des){
	 * int loopBegin,cond;
	 * Code.put(Code.pop);
	 * Code.loadConst(0);
	 * Code.loadConst(0);
	 * 
	 * loopBegin=Code.pc;    -pocetak petlje
	 * Code.put(Code.dup);
	 * Code.load(des);
	 * Code.put(Code.arraylength);
	 * Code.put(FlaseJump(Code.lt,0);  -ovde skace na kraj
	 * cond=Code.pc-2;
	 * 
	 * Code.put(Code.dup_x1);
	 * Code.load(des);
	 * Code.put(Code.dup_x1);
	 * Code.put(Code.pop);
	 * Code.put(Code.aload);
	 * Code.put(Code.add);
	 * Code.put(Code.dup_x1);
	 * Code.put(Code.pop);
	 * Code.loadConst(1);
	 * Code.put(Code.add);
	 * Code.putJump(loopBegin); -skoci na pocetak petlje
	 *  
	 * Code.fixup(cond);  -skoci na kraj
	 * Code.put(Code.pop):
	 * }
	 */
	//MAX NIZA
	/*public void aaraymax(Obj des){
	 * int loopBegin,endcond,notmax;
	 * 
	 * Code.loadConst(0);
	 * Code.put(Code.aload);
	 * Code.loadConst(1);
	 * 
	 * loopBegin=Code.pc;    -pocetak petlje
	 * Code.put(Code.dup);
	 * Code.load(des);
	 * Code.put(Code.arraylength);
	 * Code.FalseJump(Code.ne,0);  -ovde skace na kraj
	 * endcond=Code.pc-2;
	 * 
	 * Code.put(Code.dup_x1);
	 * Code.load(des);
	 * Code.put(Code.dup_x1);
	 * Code.put(Code.pop);
	 * Code.put(Code.aload);
	 * Code.put(Code.dup2);
	 * 
	 * Code.putFalseJump(Code.lt,0); -skoci na nije veci
	 * notmax=Code.pc-2;
	 * 
	 * Code.put(Code.dup_x1);
	 * Code.put(Code.pop);
	 * Code.fixup(notmax);
	 * 
	 * 
	 * Code.put(Code.pop);
	 * Code.put(Code.dup_x1); 
	 *  Code.loadConst(1);
	 * Code.put(Code.add);  
	 * Code.putJump(loopBegin); -skoci na pocetak petlje
	 * 
	 * Code.fixup(endCond); -skoci na kraj
	 * Code.put(Code.pop);
	 * }
	 * 
	 * 
	 * /*public void aarayaverage(Obj des){
	 * int loopBegin,cond;
	 * Code.put(Code.pop);
	 * Code.loadConst(0);
	 * Code.loadConst(0);
	 * 
	 * loopBegin=Code.pc;    -pocetak petlje
	 * Code.put(Code.dup);
	 * Code.load(des);
	 * Code.put(Code.arraylength);
	 * Code.put(FlaseJump(Code.ne,0);  -ovde skace na kraj
	 * cond=Code.pc-2;
	 * 
	 * Code.put(Code.dup_x1);
	 * Code.load(des);
	 * Code.put(Code.dup_x1);
	 * Code.put(Code.pop);
	 * Code.put(Code.aload);
	 * Code.put(Code.add);
	 * Code.put(Code.dup_x1);
	 * Code.put(Code.pop);
	 * Code.loadConst(1);
	 * Code.put(Code.add);
	 * Code.putJump(loopBegin); -skoci na pocetak petlje
	 *  
	 * Code.fixup(cond);  -skoci na kraj
	 * Code.put(Code.div):
	 * }
	 */
	 //modifikacije
	/* +	//MAJA 1296124481
	//ZIZA 1514756673
	//niz@ 1296124481 ili 1514756673
	public void visit(HelpModification help) {
		for(int i=0;i<4;i++) {
			Code.load(help.getDesignator().obj);
			Code.loadConst(i);
		}
	} 
	
	public void visit(Modification mod) {
		Obj con = new Obj(Obj.Var,"$",Tab.intType);
		Code.store(con);
		
		Code.load(con);
		Code.put(Code.bastore);
		int x = 8;
		for(int i=0;i<3;i++) {
			Code.load(con);
			Code.loadConst(x);
			x+=8;
			Code.put(Code.shr);
			Code.put(Code.bastore);
		}
	}
		

+	//za i = 1; niz@1 = niz[1] + niz[5-1]; gde je 5 arraylength
	public void visit(Modification mod) {
		//niz[i]
		Code.load(mod.getDesignator().obj);
		Code.loadConst(mod.getN2());
		Code.put(Code.aload);
		
		Code.load(mod.getDesignator().obj);
		
		//izracunam n
		Code.load(mod.getDesignator().obj);
		Code.put(Code.arraylength);
		
		Code.loadConst(mod.getN2());
		
		Code.put(Code.sub);
		Code.put(Code.aload);
		Code.put(Code.add);
		
		
		Code.loadConst(5);
		Code.put(Code.print);
	}

+	//za #niz[3] => broj ponavljanja
	public void visit(DesignatorArray desArr) {
		Code.put(Code.dup2);
		Code.put(Code.dup2);
		Code.put(Code.pop);
		Code.put(Code.arraylength);
		Code.loadConst(2);
		Code.put(Code.div);
		Code.put(Code.add);
		Code.put(Code.dup2);
		Code.put(Code.aload);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.put(Code.dup);
		Code.loadConst(5);
		Code.put(Code.print);
		Code.put(Code.astore);
	}
	
	public void visit(FactorNew fNew) {
		Code.put(Code.dup);
		Code.put(Code.add);
		Code.put(Code.newarray);
		if(fNew.struct.getKind() == Struct.Int) Code.put(1);
		else Code.put(0);
	}

+	//niz['a','b','c'] i kad se pozove niz[5]@4 => niz[(5+4)%arraylength]
	public void visit(Modification mod) {
		DesignatorArray des = (DesignatorArray)mod.getDesignator();
		Code.put(Code.add);
		Code.load(des.getArrayPrepare().obj);
		Code.put(Code.arraylength);
		Code.put(Code.rem);
		Code.put(Code.aload); ili baload za char
		
		Code.loadConst(5); ili Code.put(loadConst(1));
		Code.put(Code.print); ili Code.put(Code.bprint);
	}

+	//Cezarov algoritam - svako slovo se menja sa odg slovom pomerenim za odg broj ???

+	//februarska modif sa labelama

+ 	//niz @ da ispise max nekog niza
	int endAddr1, endAddr2, whileAddr;
	public void visit(Modification mod) {
		// 2 3 1 5 0 cnt max
		
		//max da je prvi el. 
		Code.load(mod.getDesignator().obj);
		Code.load(mod.getDesignator().obj);
		Code.put(Code.arraylength);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.load(mod.getDesignator().obj);
		Code.loadConst(0);
		Code.put(Code.aload);
		Code.put(Code.astore);
			
		//cnt
		Code.load(mod.getDesignator().obj);
		Code.load(mod.getDesignator().obj);
		Code.put(Code.arraylength);
		Code.loadConst(2);
		Code.put(Code.sub);
		Code.loadConst(1);
		Code.put(Code.astore);
		
		
		whileAddr = Code.pc; */
	

