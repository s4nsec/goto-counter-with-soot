# goto() counter
The main objective of this project is to determine the number of goto instructions executed at runtime.

Firstly, we write code to identify the main class in the code. For that, we utilize Scene.v().getMainClass() method to return Scene's idea of main class. Subsequently, we call declaresMethod() to find the subsignature of main method, which is void main(java.lang.String[]).
```Java
if(!Scene.v().getMainClass().declaresMethod("void main(java.lang.String[])")){
	System.Out.println("No main() method is found");
}
```

Next up, we check that if we have a gotoCount field in our main method. If yes, we are going to get it. If not, we are going to add the field.
```Java
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
```
Following up, we check if the body is the body of the main method:
```Java
boolean isMainMethod = body.getMethod().getSubSignature().equals("void main(java.lang.String[])");
```
Then, we add a new local to the body of the main method:
```Java
Local tmpLocal = Jimple.v().newLocal("tmp", LongType.v());
body.getLocals().add(tmpLocal);
```

Now, we iterate through units. We use body.getUnits() to get chain of units:
```Java
units = body.getUnits();
Iterator stmtIt = units.snapshotIterator();
```

Next step is to iterate through the iterator, determine the statement type, and if it is a GotoStmt, add one to the gotoCounter variable, then add the addition statement to the body.
If we encounter System.exit() function, we have to print gotoCounter's status before the call. To do that we get our target function - exit() via getMethod() call, and make sure it is our desired function with getSignature() call. We also need to print gotoCounter value if we encounter a return
```Java
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
```
