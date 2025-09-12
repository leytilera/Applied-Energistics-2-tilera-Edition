package appeng.transformer.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class InterfaceMethodRepairer {

    protected String thisClass;
    protected boolean isInterface = false;

    public InterfaceMethodRepairer(String className) {
        this.thisClass = className;
    }

    public abstract void visitMethod(String name, String desc);

    public abstract void generateMethods(ClassVisitor cv);

    public void markInterface() {
        isInterface = true;
    }

    protected void invokeMethod(MethodVisitor mv, String owner, String name, String desc) {
        mv.visitMethodInsn(isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, owner, name, desc, isInterface);
    }
}
