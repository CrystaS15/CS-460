package ModifierChecker;

import AST.*;
import Utilities.*;
import NameChecker.*;
import TypeChecker.*;
import Utilities.Error;

public class ModifierChecker extends Visitor {

	private SymbolTable classTable;
	private ClassDecl currentClass;
	private ClassBodyDecl currentContext;
	private boolean leftHandSide = false;

	public ModifierChecker(SymbolTable classTable, boolean debug) {
		this.classTable = classTable;
		this.debug = debug;
	}

	/** Assignment */
	public Object visitAssignment(Assignment as) {
	    println(as.line + ": Visiting an assignment (Operator: " + as.op()+ ")");

		boolean oldLeftHandSide = leftHandSide;

		leftHandSide = true;
		as.left().visit(this);

		// Added 06/28/2012 - no assigning to the 'length' field of an array type
		if (as.left() instanceof FieldRef) {
			FieldRef fr = (FieldRef)as.left();
			if (fr.target().type.isArrayType() && fr.fieldName().getname().equals("length"))
				Error.error(fr,"Cannot assign a value to final variable length.");
		}

		leftHandSide = oldLeftHandSide;
		as.right().visit(this);

		return null;
	}

	/** CInvocation */
	public Object visitCInvocation(CInvocation ci) {
	    println(ci.line + ": Visiting an explicit constructor invocation (" + (ci.superConstructorCall() ? "super" : "this") + ").");

		// YOUR CODE HERE

	    // An explicit constructor invocation through the super keyword cannot access a private contructor
	    // in the super-class 
	    if ((ci.constructor.getModifiers()).isPrivate()) {
		Error.error(ci, "cannot access private constructor in super-class");
	    }

		return null;
	}

	/** ClassDecl */
	public Object visitClassDecl(ClassDecl cd) {
		println(cd.line + ": Visiting a class declaration for class '" + cd.name() + "'.");

		currentClass = cd;

		// If this class has not yet been declared public make it so.
		if (!cd.modifiers.isPublic())
			cd.modifiers.set(true, false, new Modifier(Modifier.Public));

		// If this is an interface declare it abstract!
		if (cd.isInterface() && !cd.modifiers.isAbstract())
			cd.modifiers.set(false, false, new Modifier(Modifier.Abstract));

		// If this class extends another class then make sure it wasn't declared
		// final.
		if (cd.superClass() != null)
			if (cd.superClass().myDecl.modifiers.isFinal())
				Error.error(cd, "Class '" + cd.name()
						+ "' cannot inherit from final class '"
						+ cd.superClass().typeName() + "'.");

		// YOUR CODE HERE

		return null;
	}

	/** FieldDecl */
	public Object visitFieldDecl(FieldDecl fd) {
	    println(fd.line + ": Visiting a field declaration for field '" +fd.var().name() + "'.");

		// If field is not private and hasn't been declared public make it so.
		if (!fd.modifiers.isPrivate() && !fd.modifiers.isPublic())
			fd.modifiers.set(false, false, new Modifier(Modifier.Public));

		// YOUR CODE HERE
		currentContext = fd;
		
		// Since final fields cannot be re-assigned they must be initialized at declaration time
		if (fd.modifiers.isFinal()) {
		    fd.modifiers.set(false, false, new Modifier(Modifier.Final));
		}

		return null;
	}

	/** FieldRef */
	public Object visitFieldRef(FieldRef fr) {
	    println(fr.line + ": Visiting a field reference '" + fr.fieldName() + "'.");

		// YOUR CODE HERE

	    // References of non-static fields through a FieldRef is illegal
	    if (!(fr.myDecl.modifiers.isStatic())) {
		Error.error(fr, "non-static field '" + fr.fieldName() + 
			                                          "' cannot be referenced from a static context.");
	    }

	    // References like super.field and this.field is illegal from a static context
	    
	    // In an Assignment, a final field cannot be re-assigned
	    if (!fr.myDecl.modifiers.isFinal()) {
		Error.error(fr, "Cannot assign a value to final field '" + fr.fieldName() + "'.");
	    }

	    // Private fields can only be accessed if the target of the field reference is of the same 
	    // type (class) as that of the context.
	    if (fr.targetType.isClassType()) {
		Error.error(fr, "field '" + fr.fieldName() + 
			                    "' was declared 'private' and cannot be accessed outside its class.");
	    }

		return null;
	}

	/** MethodDecl */
	public Object visitMethodDecl(MethodDecl md) {
	    println(md.line + ": Visiting a method declaration for method '" + md.name() + "'.");

		// YOUR CODE HERE
	    currentContext = md;
	    
	    // Re-implementation of a static (non-static) method without declaring it static (non-static) 
	    // when re-implementing is illegal.
	    /*
	    if (!md.isStatic()) {
		Error.error(md, "re-implementation of static method without delcaring it static is illegal");
	    }
	    */

	    // References like super.method or this.method is illegal from a static context
	    /*
	    if (md.modifiers() instanceof Super || md.modifiers() instanceof This) {
		Error.error(md, 
			      "References like super.method() or this.method() is illegal from a static context");
	    }
	    */
	    
	    // A re-implementation of a method is only legal if the method is not already declared final 
	    // in the super class
	    if ((md.getMyClass()).superClass() != null) {
		if ((md.getMyClass()).superClass().myDecl.modifiers.isFinal()) {
		    Error.error(md, "re-implementation of a method is illegal if declared final in super class");
		}
	    }

		return null;
	}

	/** Invocation */
	public Object visitInvocation(Invocation in) {
	    println(in.line + ": Visiting an invocation of method '" + in.methodName() + "'.");

		// YOUR CODE HERE

	    // An Invocation of a non-static method from a static context is illegal
	    if (!in.targetMethod.isStatic()) {
		Error.error(in, "non-static method '" + in.methodName() + 
			                                        "' cannot be referenced from a static context.");
	    }
	    
	    // Invocation of a private method is only legal if current context is the same type (class) containing
	    // the method to be invoked
	    if (currentClass == in.targetMethod.getMyClass()) {
		Error.error(in, "current context is the same type as the method invoked");
	    }

		return null;
	}


	public Object visitNameExpr(NameExpr ne) {
	    println(ne.line + ": Visiting a name expression '" + ne.name() + "'. (Nothing to do!)");
	    return null;
	}

	/** ConstructorDecl */
	public Object visitConstructorDecl(ConstructorDecl cd) {
	    println(cd.line + ": visiting a constructor declaration for class '" + cd.name() + "'.");

		// YOUR CODE HERE
	    currentContext = cd;

		return null;
	}

	/** New */
	public Object visitNew(New ne) {
	    println(ne.line + ": visiting a new '" + ne.type().myDecl.name() + "'.");

		// YOUR CODE HERE

	    // When instantiating a class, the constructor called cannot be private
	    if (((ne.getConstructorDecl()).getModifiers()).isPrivate()) {
		Error.error(ne, ne.type().myDecl.name() + "( ) has private access in '" + ne.type().myDecl.name() 
			                                                                                  + "'.");
	    }

		return null;
	}

	/** StaticInit */
	public Object visitStaticInitDecl(StaticInitDecl si) {
		println(si.line + ": visiting a static initializer");

		// YOUR CODE HERE
		currentContext = si;

		return null;
	}

	/** Super */
	public Object visitSuper(Super su) {
		println(su.line + ": visiting a super");

		if (currentContext.isStatic())
			Error.error(su,
					"non-static variable super cannot be referenced from a static context");

		return null;
	}

	/** This */
	public Object visitThis(This th) {
		println(th.line + ": visiting a this");

		if (currentContext.isStatic())
			Error.error(th,	"non-static variable this cannot be referenced from a static context");

		return null;
	}

	/** UnaryPostExpression */
    public Object visitUnaryPostExpr(UnaryPostExpr up) {
	println(up.line + ": visiting a unary post expression with operator '" + up.op() + "'.");
	
	// YOUR CODE HERE
	up.expr().visit(this);
	
	// A final field cannot be re-assigned
	if (up.expr() instanceof FieldRef) {
	    FieldRef fr = (FieldRef)up.expr();
	    if (fr.myDecl.modifiers.isFinal()) {
		Error.error(fr, "Cannot assign a value to final field '" + fr.fieldName() + "'.");
	    }
	}


	    
	return null;
    }
    
    /** UnaryPreExpr */
    public Object visitUnaryPreExpr(UnaryPreExpr up) {
	println(up.line + ": visiting a unary pre expression with operator '" + up.op() + "'.");
	
	// YOUR CODE HERE
	up.expr().visit(this);
	
	// A final field cannot be re-assigned
	if (up.expr() instanceof FieldRef) {
	    FieldRef fr = (FieldRef)up.expr();
	    if (fr.myDecl.modifiers.isFinal()) {
		Error.error(fr, "Cannot assign a value to final field '" + fr.fieldName() + "'.");
	    }
	}

	return null;
    }
}
