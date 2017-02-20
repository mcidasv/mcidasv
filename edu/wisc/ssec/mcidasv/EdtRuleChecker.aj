package edu.wisc.ssec.mcidasv;

import java.awt.Window;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public aspect EdtRuleChecker {
    
    
    private final AtomicInteger violationCount = new AtomicInteger(0);
    
    private boolean isStressChecking = true;
    
    public pointcut anyEdtMethods(Window w):
        target(w) && call(* *(..));
    
    public pointcut anySwingMethods(JComponent c):
        target(c) && call(* *(..));
    
    public pointcut threadSafeMethods():
//        call(* showStats()) ||
        call(* repaint(..)) ||
        call(* revalidate()) ||
        call(* invalidate()) ||
        call(* getListeners(..)) ||
        call(* add*Listener(..)) ||
        call(* remove*Listener(..));
    
    //calls of any JComponent method, including subclasses
    before(JComponent c): anySwingMethods(c) &&
        !threadSafeMethods() &&
        !within(EdtRuleChecker) {
        if(!SwingUtilities.isEventDispatchThread() &&
            (isStressChecking || c.isShowing()))
        {
            int count = violationCount.incrementAndGet();
            System.err.println(thisJoinPoint.getSourceLocation() + " (#" + count + ')');
            System.err.println(thisJoinPoint.getSignature());
            System.err.println();
        }
    }
    
    // handle AWT Window (and subclasses, which includes JFrames, JDialogs,
    // etc)
    before(Window w): anyEdtMethods(w) && !threadSafeMethods() && !within(EdtRuleChecker) {
        if (!SwingUtilities.isEventDispatchThread() && (isStressChecking || w.isShowing())) {
            int count = violationCount.incrementAndGet();
            System.err.println(thisJoinPoint.getSourceLocation() + " (#" + count + ')');
            System.err.println(thisJoinPoint.getSignature());
            System.err.println();
        }
    }
    
    //calls of any JComponent constructor, including subclasses
    before(): call(JComponent+.new(..)) {
        if (isStressChecking && !SwingUtilities.isEventDispatchThread()) {
            int count = violationCount.incrementAndGet();
            System.err.println(thisJoinPoint.getSourceLocation() + " (#" + count + ')');
            System.err.println(thisJoinPoint.getSignature() + " *constructor*");
            System.err.println();
        }
    }
    
    // calls of any AWT constructors, including subclasses
    before(): call(Window+.new(..)) {
        if (isStressChecking && !SwingUtilities.isEventDispatchThread()) {
            int count = violationCount.incrementAndGet();
            System.err.println(thisJoinPoint.getSourceLocation() + " (#" + count + ')');
            System.err.println(thisJoinPoint.getSignature() + " *constructor*");
            System.err.println();
        }
    }
}