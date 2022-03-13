import soot.*;
import soot.jimple.*;
import soot.util.*;
import java.io.*;
import java.util.*;

public class Main
{    
    public static void main(String[] args) 
    {
        Scene.v().getPack("jtp").add(new Transform("jtp.instrumenter", GotoInstrumenter.v()));
        soot.Main.main(args);
    }
}

class GotoInstrumenter extends BodyTransformer
{
    private static GotoInstrumenter instance = new GotoInstrumenter();
    private GotoInstrumenter() {}

    public static GotoInstrumenter v() { 
        return instance; 
    }

    public String getDeclaredOptions() { 
        return super.getDeclaredOptions(); 
    }

    private boolean isFieldAdded = false;

    private SootClass javaIoPrintStream;

    private Local printReferenceType(Body body)
    {
        Local printRefType = Jimple.v().newLocal("printRefType", RefType.v("java.io.PrintStream"));
        body.getLocals().add(printRefType);
        return printRefType;
    }
     
    private Local defineLongVar(Body body)
    {
        Local longVarLocal = Jimple.v().newLocal("longVarLocal", LongType.v()); 
        body.getLocals().add(longVarLocal);
        return longVarLocal;
    }

    private void insertStatements(Chain units, Stmt statement, SootField gotoCounter, Local printRefType, Local longVarLocal)
    {
        units.insertBefore(Jimple.v().newAssignStmt(printRefType, Jimple.v().newStaticFieldRef( Scene.v().getField("<java.lang.System: java.io.PrintStream out>"))), statement);
        units.insertBefore(Jimple.v().newAssignStmt(longVarLocal, Jimple.v().newStaticFieldRef(gotoCounter)), statement);
        SootMethod invokePrint = javaIoPrintStream.getMethod("void println(long)");                    
        units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(printRefType, invokePrint, longVarLocal)), statement);
    }

    protected void internalTransform(Body body, String phaseName, Map options)
    {
        SootClass sootClass = body.getMethod().getDeclaringClass();
        SootField gotoCounter = null;
        boolean addedLocals = false;
        Local printRefType = null, longVarLocal = null;
        Chain units = body.getUnits();
        
        synchronized(this)
        {
            if (!Scene.v().getMainClass().declaresMethod("void main(java.lang.String[])")){
                System.Out.println("No main() method is found");
            }

            if (isFieldAdded){
                gotoCounter = Scene.v().getMainClass().getFieldByName("gotoCount");
            }
            else
            {
                gotoCounter = new SootField("gotoCount", LongType.v(), Modifier.STATIC);
                Scene.v().getMainClass().addField(gotoCounter);

                Scene.v().loadClassAndSupport("java.io.PrintStream");
                javaIoPrintStream = Scene.v().getSootClass("java.io.PrintStream");

                isFieldAdded = true;
            }
        }
            
        {
            boolean isMainMethod = body.getMethod().getSubSignature().equals("void main(java.lang.String[])");

            Local storeGoToCounter = Jimple.v().newLocal("tmp", LongType.v());
            body.getLocals().add(storeGoToCounter);
                
            Iterator iterator = units.snapshotIterator();
            
            while(iterator.hasNext())
            {
                Stmt statement = (Stmt) iterator.next();

                if(statement instanceof GotoStmt)
                {
                    AssignStmt statement1 = Jimple.v().newAssignStmt(storeGoToCounter, Jimple.v().newStaticFieldRef(gotoCounter));
                    AssignStmt statement2 = Jimple.v().newAssignStmt(storeGoToCounter, Jimple.v().newAddExpr(storeGoToCounter, LongConstant.v(1L)));
                    AssignStmt statement3 = Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(gotoCounter), storeGoToCounter);

                    units.insertBefore(statement1, statement); 
                    units.insertBefore(statement2, statement);
                    units.insertBefore(statement3, statement);
                }
                else if (statement instanceof InvokeStmt)
                {
                    InvokeExpr invokeExpression = (InvokeExpr) ((InvokeStmt)statement).getInvokeExpr();
                    if (invokeExpression instanceof StaticInvokeExpr)
                    {
                        SootMethod target = ((StaticInvokeExpr)invokeExpression).getMethod();
                        
                        if (target.getSignature().equals("<java.lang.System: void exit(int)>"))
                        {
                            if (!addedLocals)
                            {
                                printRefType = printReferenceType(body); longVarLocal = defineLongVar(body);
                                addedLocals = true;
                            }
                            insertStatements(units, statement, gotoCounter, printRefType, longVarLocal);
                        }
                    }
                }
                else if (isMainMethod && (statement instanceof ReturnStmt || statement instanceof ReturnVoidStmt))
                {
                    if (!addedLocals)
                    {
                        printRefType = printReferenceType(body); 
                        longVarLocal = defineLongVar(body);
                        addedLocals = true;
                    }
                    insertStatements(units, statement, gotoCounter, printRefType, longVarLocal);
                }
            }
        }
    }
}














