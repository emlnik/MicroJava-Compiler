package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;




import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;


public class SemanticPass extends VisitorAdaptor {
	
	
    private boolean errorDetected = false;
    
    private Obj currentMethod =null;
    private List<Struct> actPars= new ArrayList<>();
    private List<Struct> formPars= new ArrayList<>();
    
    private Struct returnType = Tab.noType;
    private int flagDoWhile=0;
    private boolean mainMethod = false;
    public int nVars = 0;
 
	public static Struct boolType = Tab.insert(Obj.Type, "bool", new Struct(Struct.Bool)).getType();
	private Struct currentType = Tab.noType;
	
	private static Logger log = Logger.getLogger("info");
	private static Logger logError = Logger.getLogger("error");
	
	
	
	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		logError.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}
	
	
	
	

// VISIT METODE 
	
	//ProgName
	public void visit(ProgName progName) {
		progName.obj = Tab.insert(Obj.Prog, progName.getProgName(), Tab.noType);
		Tab.openScope();
	}
	
	//Program
	public void visit(Program program) {
		if (!mainMethod) {
    		report_info("Semanticka greska: U programu mora postojati metoda 'void main();'", program);
    	}
		Tab.chainLocalSymbols(program.getProgName().obj);
		Tab.closeScope();
		
	}
	
	//Type
	public void visit(Type type) {
		Obj typeNode = Tab.find(type.getTypeName());
		if(typeNode==Tab.noObj) {
			report_error("Nije pronadjen tip " + type.getTypeName()+ " u tabeli simbla! ",null);
			currentType = type.struct=Tab.noType;
		}
		else {
			if(Obj.Type==typeNode.getKind()) {
				currentType = type.struct=typeNode.getType();
			}
			else {
				report_error("Semanticka greska: Ime "+ type.getTypeName() + " ne predstavlja tip!",type);
				currentType = type.struct=Tab.noType;
				
			}
		}
	}
	//Const
	public void visit(NumConst numConst) {
		Obj constObj = Tab.find(numConst.getName());
		if(constObj != Tab.noObj) {
			report_error("Semanticka greska: Ime konstante"+ numConst.getName() + " je vec deklarisano",numConst);
		}
		else if(currentType != Tab.intType) {
			report_error("Semanticka greska: Ime konstante"+ numConst.getName() + " nije tipa INT",numConst);
		}
		else {
			constObj = Tab.insert(Obj.Con, numConst.getName(), currentType);
			constObj.setAdr(numConst.getValue());
		}
	}
	
	public void visit(CharConst charConst) {
		Obj constObj = Tab.find(charConst.getName());
		if(constObj != Tab.noObj) {
			report_error("Semanticka greska: Ime konstante"+ charConst.getName() + " je vec deklarisano",charConst);
		}
		else if(currentType != Tab.charType) {
			report_error("Semanticka greska: Ime konstante"+ charConst.getName() + " nije tipa CHAR",charConst);
		}
		else {
			constObj = Tab.insert(Obj.Con, charConst.getName(), currentType);
			constObj.setAdr(charConst.getValue());
		}
	}
	
	public void visit(BoolConst boolConst) {
		Obj constObj = Tab.find(boolConst.getName());
		if(constObj != Tab.noObj) {
			report_error("Semanticka greska: Ime konstante"+ boolConst.getName() + " je vec deklarisano",boolConst);
		}
		else if(currentType != boolType) {
			report_error("Semanticka greska: Ime konstante"+ boolConst.getName() + " nije tipa BOOL",boolConst);
		}
		else {
			constObj = Tab.insert(Obj.Con, boolConst.getName(), currentType);
			constObj.setAdr(boolConst.getValue());
		}
	}
	
	//Variable
	
	public void visit(VarIdent varIdent) {
		Obj varObj = Tab.find(varIdent.getName());
		if(varObj != Tab.noObj) {
			report_error("Semanticka greska: Ime variable "+ varIdent.getName() + " je vec deklarisano",varIdent);
		}
		else {
			if(varIdent.getBrcs() instanceof NoBrackets) {
				varObj = Tab.insert(Obj.Var, varIdent.getName(), currentType);
				nVars++;
			}
			else if(varIdent.getBrcs() instanceof Brackets) {
				varObj = Tab.insert(Obj.Var, varIdent.getName(), new Struct(Struct.Array, currentType));
				nVars++;
			}
			else
				report_error("Semanticka greska: VarIdent",varIdent);
		}
	}
	
	public void visit(VarIdentLocal varIdentLocal) {
		Obj varObj = Tab.find(varIdentLocal.getName());
		if(varObj != Tab.noObj) {
			report_error("Semanticka greska: Ime variable "+ varIdentLocal.getName() + " je vec deklarisano",varIdentLocal);
		}
		else {
			if(varIdentLocal.getBrcs() instanceof NoBrackets)
				varObj = Tab.insert(Obj.Var, varIdentLocal.getName(), currentType);
			else if(varIdentLocal.getBrcs() instanceof Brackets)
				varObj = Tab.insert(Obj.Var, varIdentLocal.getName(), new Struct(Struct.Array, currentType));
			else
				report_error("Semanticka greska: VarIdent",varIdentLocal);
		}
	}
	
	//MethRetType
	public void visit(MthRetTypeClass mthRetTypeClass) {
		returnType=currentType;
		
	}
	
	public void visit(MthRetTypeVoid mthRetTypeVoid) {
		returnType=Tab.noType;
		
	}
	
	public void visit(MthRetTypeAndName mthRetTypeAndName) {

		Obj obj = Tab.find(mthRetTypeAndName.getMthName());
		if(obj != Tab.noObj) {
			report_error("Semanticka greska: Ime fje "+ mthRetTypeAndName.getMthName()+"vec postoji!",mthRetTypeAndName);
		}
		else {
				obj = Tab.insert(Obj.Meth, mthRetTypeAndName.getMthName(), currentType);
				currentMethod=obj;
			
				Tab.openScope();
				if (mthRetTypeAndName.getMthName().equals("main")) {
					mainMethod = true;
				}
		}
		
		mthRetTypeAndName.obj = obj;
	}
	public void visit(MethodDeclaration meth) {
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		currentMethod=null;
	}
	
	//FormPars
	
	public void visit(FormPar formPar) {
		Obj obj = Tab.find(formPar.getName());
		if(obj !=Tab.noObj) {
			report_error("Semanticka greska: Losa deklaracija formalnog parametra", formPar);
		
		}
		else {
			if(formPar.getBrcs() instanceof NoBrackets) {
				obj = Tab.insert(Obj.Var, formPar.getName(), currentType);
				obj.setFpPos(1); //znaci da je formalni parametar, a ne lokalna metoda
			}
			else if(formPar.getBrcs() instanceof Brackets) {
				obj = Tab.insert(Obj.Var, formPar.getName(), new Struct(Struct.Array, currentType));
			    obj.setFpPos(1);
			}
			else
				report_error("Semanticka greska: VarIdent",formPar);
		}
		
		
	}
	
	
	
	
	//Statement
	
	public void visit(StatementDoWhile stmDoWhile) {
		flagDoWhile--;
	}
	
	public void visit(DoTerm doT) {
		flagDoWhile++;
	}
	
	public void visit(StatementBreak stmBreak) {
		if(flagDoWhile==0) {
			report_error("Semanticka greska: Break nije unutar petlje!", stmBreak);
		}
	}
	
	public void visit(StatementContinue stmContinue) {
		if(flagDoWhile==0) {
			report_error("Semanticka greska: Continue nije unutar petlje!", stmContinue);
		}
	}
	
	public void visit(StatementReturnExpr stmReturn) {
		
		if(currentMethod==null) {
			report_error("Semanticka greska: Return se ne nalazi u okviru metode!", stmReturn);
			return;
		}
		
		
		if(returnType!= stmReturn.getExpr().struct) {
			report_error("Semanticka greska: Povratni tip i tip metode nisu isti!", stmReturn);
			return;
		}
		
		stmReturn.struct=stmReturn.getExpr().struct;
		
	}
	
	public void visit(StatementNoReturnExpr stm) {
			
			if(currentMethod==null) {
				report_error("Semanticka greska: Return se ne nalazi u okviru metode!", stm);
				return;
			}
			if(returnType!= Tab.noType) {
				report_error("Semanticka greska: Povratni tip nije void!", stm);
				return;
			}
	}
	
	public void visit(StatementRead stmRead) {
		if(stmRead.getDesignator().obj.getKind() == Obj.Var || stmRead.getDesignator().obj.getKind() == Obj.Elem) {
			if(!stmRead.getDesignator().obj.getType().equals(Tab.intType) && !stmRead.getDesignator().obj.getType().equals(Tab.charType) && !stmRead.getDesignator().obj.getType().equals(boolType)) {
				report_error("Semanticka greska: Izraz mora biti tipa int, char ili bool", stmRead );
			}
		}
		else {
			report_error("Semanticka greska: Argument mora biti promenljiva ili element niza!", stmRead);
		}
		
	}
	
	public void visit(StatementPrint stmPrint) { //print numconst
		if(!stmPrint.getExpr().struct.equals(Tab.intType) && !stmPrint.getExpr().struct.equals(Tab.charType) && !stmPrint.getExpr().struct.equals(boolType)) {
			report_error("Semanticka greska: Izraz mora biti tipa int, char ili bool!", stmPrint);
		}
	}
	
	public void visit(StatementNoPrintNumconst stmNoPrintNumConst) {
		if(!stmNoPrintNumConst.getExpr().struct.equals(Tab.intType) && !stmNoPrintNumConst.getExpr().struct.equals(Tab.charType) && !stmNoPrintNumConst.getExpr().struct.equals(boolType)){
			report_error("Semanticka greska: Izraz mora biti tipa int, char ili bool!", stmNoPrintNumConst);
			
		}
	}
	
	//DesignatorStatement 
	public void visit(DesignatorStmAssignop designatorAssignop) {
		if(designatorAssignop.getDesignator().obj.getKind() != Obj.Var && designatorAssignop.getDesignator().obj.getKind() != Obj.Elem) {
			report_error("Semanticka greska: Vrednost samo moze da se dodeli elementu niza ili promenljivoj!", designatorAssignop);
			designatorAssignop.obj=Tab.noObj;
		}
		if(!designatorAssignop.getExpr().struct.assignableTo(designatorAssignop.getDesignator().obj.getType())) {
			report_error("Semanticka greska: Tipovi za dodelu nisu kompatibilni!", designatorAssignop);
			designatorAssignop.obj=Tab.noObj;
		}
		else {
			designatorAssignop.obj=designatorAssignop.getDesignator().obj;
		}
		
	}
	
	public void visit(DesignatorStmInc designatorInc) {
		if(designatorInc.getDesignator().obj.getType() != Tab.intType) {
			report_error("Semanticka greska: Tip mora biti int!", designatorInc);
		designatorInc.obj=Tab.noObj;
		}
		else {
			designatorInc.obj=designatorInc.getDesignator().obj;
		}
	}
	
	public void visit(DesignatorStmDec designatorDec) {
		if(designatorDec.getDesignator().obj.getType() !=Tab.intType) {
			report_error("Semanticka greska: Tip mora biti int!", designatorDec);
			designatorDec.obj=Tab.noObj;
		}
		else {
		designatorDec.obj=designatorDec.getDesignator().obj;
		}
	}
	
	public void visit(DesignatorStmActPars desPars) {
		if(desPars.getDesignator().obj.getKind() != Obj.Meth) {
			report_error("Semanticka greska: Mora biti metoda!", desPars);
			desPars.obj=Tab.noObj;
		}
		else {
			desPars.obj=desPars.getDesignator().obj;
		}
	}

	
	
	//ActPars
	
	public void visit(ActParsSingleExpr actParsSingle) {
		actPars.add(actParsSingle.getExpr().struct);
	}
	
	public void visit(ActParsMoreExpr actParsMore) {
		actPars.add(actParsMore.getExpr().struct);
	}
	
	public void visit(ActParamas actParams) {
		
		if(actParams.getParent() instanceof DesignatorStmActPars) {
			for(Obj o: ((DesignatorStmActPars)actParams.getParent()).getDesignator().obj.getLocalSymbols()) {
				if(o.getFpPos() == 1)
					formPars.add(o.getType());
			}
		}
		else if (actParams.getParent() instanceof FactorDesignatorActPars){
			for(Obj o: ((FactorDesignatorActPars)actParams.getParent()).getDesignator().obj.getLocalSymbols()) {
				if(o.getFpPos() == 1)
					formPars.add(o.getType());
			}
		}
		
		
		if(formPars.size()!=actPars.size())
			report_error("Semanticka greska: Nije isti broj formalnih i act parametara!", actParams);
		else {
		   for(int i=0; i< actPars.size();i++) {
			   if(!actPars.get(i).assignableTo(formPars.get(i))) {
				   report_error("Semanticka greska: Act i formal parametri nisu isti!", actParams);
				   return;
			   }
		   }
		}
		actPars.clear();
		formPars.clear();
	}
	public void visit(NoActParamas actParams) {
		if(actParams.getParent() instanceof DesignatorStmActPars) {
			for(Obj o: ((DesignatorStmActPars)actParams.getParent()).getDesignator().obj.getLocalSymbols()) {
				if(o.getFpPos() == 1)
					formPars.add(o.getType());
			}
		}
		else if (actParams.getParent() instanceof FactorDesignatorActPars){
			for(Obj o: ((FactorDesignatorActPars)actParams.getParent()).getDesignator().obj.getLocalSymbols()) {
				if(o.getFpPos() == 1)
					formPars.add(o.getType());
			}
		}
		
		if(formPars.size()!=actPars.size())
			report_error("Semanticka greska: Nije isti broj formalnih i act parametara!", actParams);
		else {
		   for(int i=0; i< actPars.size();i++) {
			   if(!actPars.get(i).assignableTo(formPars.get(i))) {
				   report_error("Semanticka greska: Act i formal parametri nisu isti!", actParams);
				   return;
			   }
		   }
		}
		actPars.clear();
		formPars.clear();
	}
	
	
	//Condition
	public void visit(Condition con) {
		if(con.getCondTermList().struct == boolType) {
			con.struct = boolType;
		}
		else {
			report_error("Semanticka greska: Condition mora biti bool", con);
			con.struct=Tab.noType;
		}
	}
	public void visit(CondTerm condTerm) {
		condTerm.struct = condTerm.getCondFactList().struct;
	}
	
	public void visit(ConditionSingleCondTerm conditionSingle) {
		conditionSingle.struct = conditionSingle.getCondTerm().struct;
	}
	
	public void visit(ConditionMoreCondTerm conditionMore) {
		if(conditionMore.getCondTermList().struct != boolType || conditionMore.getCondTerm().struct!=boolType) {
			report_error("Semanticka greska: Oba izraza u OR operaciji moraju biti BOOL", conditionMore);
			conditionMore.struct=Tab.noType;
			
			}
			else {
				conditionMore.struct = conditionMore.getCondTerm().struct;
			}
	}
	
	//CondTerm
	
	public void visit(CondTermSingleCondFact condTermSingle) {
		condTermSingle.struct=condTermSingle.getCondFact().struct;
	}
	
	public void visit(CondTermMoreCondFact condTermMore) {//AND
		if(condTermMore.getCondFactList().struct == boolType && condTermMore.getCondFact().struct==boolType) {
		condTermMore.struct = condTermMore.getCondFact().struct;
		}
		else {
			report_error("Semanticka greska: Oba izraza u AND operaciji moraju biti BOOL", condTermMore);
			condTermMore.struct=Tab.noType;
		}
	}
	
	//CondFact
	public void visit(CondFactSingleExpr condFactSingleExpr) {
		
		condFactSingleExpr.struct= condFactSingleExpr.getExprSingle().struct;
		
	}
	
	public void visit(CondFactMoreExpr condFactMore) {
		if(!condFactMore.getExprSingle().struct.compatibleWith(condFactMore.getExprSingle1().struct)) {
			report_error("Semanticka greska: Nisu kompatibilni izrazi!", condFactMore);
			condFactMore.struct=Tab.noType;
		}
		else
		{
			if(condFactMore.getExprSingle().struct==Tab.nullType || condFactMore.getExprSingle1().struct==Tab.nullType
					|| condFactMore.getExprSingle().struct.getKind() == Struct.Array || condFactMore.getExprSingle1().struct.getKind()==Struct.Array) {
				if(!(condFactMore.getRelop() instanceof RelopEqual || condFactMore.getRelop() instanceof RelopNotEqual)) {
					report_error("Semanticka greska: Operatori u relacionom izrazu mogu biti samo == i != !", condFactMore);
					condFactMore.struct=Tab.noType;
					
				}
				else {
					condFactMore.struct = boolType;
				}
			}
			else {
				condFactMore.struct = boolType;
			}
		}
		
		
	}
	
	//Expr
	public void visit(Expression exprSingle) {
		exprSingle.struct= exprSingle.getExprSingle().struct;
	}
	
	public void visit(ExpressionTernary expressionTernary) {
		if(expressionTernary.getExprSingle().struct.compatibleWith(expressionTernary.getExpr().struct)) {
			expressionTernary.struct=expressionTernary.getExprSingle().struct;
		}
		else {
			
		
			report_error("Semanticka greska: Drugi i treci uslov nisu istog tipa!", expressionTernary);
			expressionTernary.struct=Tab.noType;
		}
	}
	
	public void visit(ExprSingleMinusTerm exprSingleMinusTerm) {
		if(!exprSingleMinusTerm.getTermList().struct.equals(Tab.intType) && exprSingleMinusTerm.getMayMinus() instanceof YesMinus) {
			report_error("Semanticka greska: elementi niza moraju biti tipa int!", exprSingleMinusTerm);
			exprSingleMinusTerm.struct=Tab.noType;
		}
		else {
	
			exprSingleMinusTerm.struct=exprSingleMinusTerm.getTermList().struct;
		}
	}
	
	//Term 
	
	public void visit(TermAddop termAddop) {
		if(termAddop.getTermList().struct.equals(Tab.intType) && termAddop.getTerm().struct.equals(Tab.intType)){
			termAddop.struct= termAddop.getTermList().struct;
		}
		else {
			report_error("Semanticka greska: elementi niza nisu tipa int!", termAddop);
			termAddop.struct = Tab.noType;
		}
	}
	
	public void visit(SingleTerm singleTerm) {
		singleTerm.struct = singleTerm.getTerm().struct;
	}
    public void visit(TermFactor termFactor) {
    	termFactor.struct=termFactor.getFactorList().struct;
    }
	//Factor 
	
	public void visit(FactorSingleDesignator factorSingleDes) {
		if(factorSingleDes.getDesignator().obj.getKind() != Obj.Var && factorSingleDes.getDesignator().obj.getKind() != Obj.Elem
				&& factorSingleDes.getDesignator().obj.getKind() != Obj.Con || factorSingleDes.getDesignator().obj == Tab.noObj) {
			report_error("Designator u FactorDesignatoru nema odgovarajuci Kind!", factorSingleDes);
			factorSingleDes.struct = Tab.noType;
		}
		else
			factorSingleDes.struct= factorSingleDes.getDesignator().obj.getType();
	}
	
	public void visit(SingleFactor singleFactor) {
		singleFactor.struct  = singleFactor.getFactor().struct;
	}
	
	public void visit(FactorNumConst factorNum) {
		factorNum.struct= Tab.intType;
	}
	
	public void visit(FactorCharConst factorChar) {
		factorChar.struct=Tab.charType;
	}
	
	public void visit(FactorBoolConst factorBool) {
		factorBool.struct=boolType;
	}
	
	public void visit(FactorNewTypeExpr factorNewExpr) {
		if(factorNewExpr.getExpr().struct != Tab.intType) {
			report_error("Semanticka greska: Tip u izrazu mora biti int!", factorNewExpr);
			factorNewExpr.struct= Tab.noType;
		}
		else {
			factorNewExpr.struct= new Struct(Struct.Array, factorNewExpr.getType().struct);
		}
	}
	
	public void visit(FactorExpr factorExpr) {
		factorExpr.struct= factorExpr.getExpr().struct;
	}
	
	public void visit(FactorDesignatorActPars desPars) {
		if(desPars.getDesignator().obj.getKind() != Obj.Meth) {
			report_error("Semanticka greska: Mora biti metoda!", desPars);
			desPars.struct=Tab.noType;
		}
		else {
			desPars.struct=desPars.getDesignator().obj.getType();
		}
	}
	public void visit(FactorListMulop factorListMulop) {
		if(factorListMulop.getFactorList().struct !=Tab.intType || factorListMulop.getFactor().struct !=Tab.intType) {
			report_error("Semanticka greska: Moraju biti tipa int!", factorListMulop);
			factorListMulop.struct=Tab.noType;
		}
		else {
			factorListMulop.struct=factorListMulop.getFactor().struct;
		}
	}
	
	//Designator
	public void visit(DesignatorArrName desArrName) {
		Obj dobj= Tab.find(desArrName.getName());
		if(dobj==Tab.noObj) {
			report_error("Semanticka greska: Niz " + desArrName.getName() + "nije deklarisan!",desArrName);
			dobj = Tab.noObj;
		}
		else if(dobj.getType().getKind() != Struct.Array) {
			report_error("Semanticka greska: [DesignatorArrName] " + desArrName.getName()+ " nije niz!", desArrName);
			dobj = Tab.noObj;
		}
		desArrName.obj=dobj;
	}
	public void visit(DesignatorIdent designatorIdent) {
		Obj desObj = Tab.find(designatorIdent.getName());
		if(desObj==Tab.noObj) {
			report_error("Semanticka greska: " + designatorIdent.getName() + " nije deklarisano!", designatorIdent);
			
		}
		
		designatorIdent.obj=desObj;
		
	}
	public void visit(DesignatorIdentArray designatorIdentArray) {
		
	    if(designatorIdentArray.getDesignatorArrName().obj == Tab.noObj)
	    	designatorIdentArray.obj = Tab.noObj;
	    else if (designatorIdentArray.getExpr().struct != Tab.intType) {
	    	report_error("Semanticka greska: Los pristup nizu!", designatorIdentArray);
	    	designatorIdentArray.obj = Tab.noObj;
	    }
	    else{
	    	designatorIdentArray.obj = new Obj(Obj.Elem, "elem", designatorIdentArray.getDesignatorArrName().obj.getType().getElemType());
	    	report_info("Pristup elementu niza " + designatorIdentArray.getDesignatorArrName().getName() + " ", designatorIdentArray);
	    }
	}
	
	public void visit(RelopEqual relop) {
		relop.struct = new Struct(Code.eq);
	}
	
	public void visit(RelopNotEqual relop) {
		relop.struct = new Struct(Code.ne);
	}
	
	public void visit(RelopGreater relop) {
		relop.struct = new Struct(Code.gt);
	}
	public void visit(RelopGreaterEqual relop) {
		relop.struct = new Struct(Code.ge);
	}
	public void visit(RelopLesser relop) {
		relop.struct = new Struct(Code.lt);
	}
	
	public void visit(RelopLesserEqual relop) {
		relop.struct = new Struct(Code.le);
	}
	

	public boolean passed() {
		return !errorDetected;
		
	}
}