package appeng.transformer.asm.repairers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import appeng.transformer.asm.InterfaceMethodRepairer;

public class GetChannelRepairer extends InterfaceMethodRepairer {

    static final String methGC = "getChannel";
    static final String descGC = "()Lappeng/api/storage/StorageChannel;";
    static final String methGSC = "getStorageChannel";
    static final String descGSC = "()Lappeng/api/storage/IStorageChannel;";

    boolean hasGSC = false;

    public GetChannelRepairer(String className) {
        super(className);
    }

    @Override
    public void visitMethod(String name, String desc) {
        if (methGSC.equals(name) && descGSC.equals(desc)) {
            hasGSC = true;
        }
    }

    @Override
    public void generateMethods(ClassVisitor cv) {
        if (!hasGSC) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methGSC, descGSC, null, null);
            mv.visitCode();
            // return this.getChannel();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            invokeMethod(mv, thisClass, methGC, descGC);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        if ("appeng/api/storage/data/IAEStack".equals(thisClass) || "appeng/api/storage/IMEInventory".equals(thisClass)) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methGC, descGC, null, null);
            mv.visitCode();
            Label labelElse = new Label();
            Label labelEnd = new Label();
            // IStorageChannel channel = this.getStorageChannel();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, thisClass, methGSC, descGSC, true);
            mv.visitVarInsn(Opcodes.ASTORE, 1);
            // if (channel instanceof StorageChannel) {
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, "appeng/api/storage/StorageChannel");
            mv.visitJumpInsn(Opcodes.IFEQ, labelElse);
            // StorageChannel c = (StorageChannel) channel;
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.CHECKCAST, "appeng/api/storage/StorageChannel");
            mv.visitJumpInsn(Opcodes.GOTO, labelEnd);
            // } else {
            mv.visitLabel(labelElse);
            // null
            mv.visitInsn(Opcodes.ACONST_NULL);
            // return
            mv.visitLabel(labelEnd);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }
    
}
