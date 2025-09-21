package appeng.transformer.asm.repairers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import appeng.transformer.asm.InterfaceMethodRepairer;

public class CellProviderRepairer extends InterfaceMethodRepairer {

    static final String methGCA = "getCellArray";
    static final String descGCANew = "(Lappeng/api/storage/IStorageChannel;I)Ljava/util/List;";
    static final String descGCAOld = "(Lappeng/api/storage/StorageChannel;)Ljava/util/List;";

    boolean hasGCA = false;

    public CellProviderRepairer(String className) {
            super(className);
    }
    
    @Override
    public void visitMethod(String name, String desc) {
        if (methGCA.equals(name) && descGCANew.equals(desc)) {
            hasGCA = true;
        }
    }

    @Override
    public void generateMethods(ClassVisitor cv) {
        if (!hasGCA) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methGCA, descGCANew, null, null);
            mv.visitCode();
            Label labelElse = new Label();
            Label labelEnd = new Label();
            // if (channel instanceof StorageChannel)
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, "appeng/api/storage/StorageChannel");
            mv.visitJumpInsn(Opcodes.IFEQ, labelElse);
            // {
            // StorageChannel c = (StorageChannel) channel;
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.CHECKCAST, "appeng/api/storage/StorageChannel");
            mv.visitVarInsn(Opcodes.ASTORE, 3);
            // return this.getCellArray(c);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            invokeMethod(mv, thisClass, methGCA, descGCAOld);
            // } else 
            mv.visitJumpInsn(Opcodes.GOTO, labelEnd);
            // {
            mv.visitLabel(labelElse);
            // return new ArrayList;
            mv.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
            // }
            mv.visitLabel(labelEnd);
            // Actual return
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        if ("appeng/api/storage/ICellProvider".equals(thisClass)) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methGCA, descGCAOld, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, thisClass, methGCA, descGCANew, true);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }
    
}
