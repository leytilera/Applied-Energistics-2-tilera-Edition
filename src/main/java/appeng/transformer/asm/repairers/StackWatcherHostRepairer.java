package appeng.transformer.asm.repairers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import appeng.transformer.asm.InterfaceMethodRepairer;

public class StackWatcherHostRepairer extends InterfaceMethodRepairer {

    static final String methOSC = "onStackChange";
    static final String descOSCNew = "(Lappeng/api/storage/data/IItemList;Lappeng/api/storage/data/IAEStack;Lappeng/api/storage/data/IAEStack;Lappeng/api/networking/security/BaseActionSource;Lappeng/api/storage/IStorageChannel;I)V";
    static final String descOSCOld = "(Lappeng/api/storage/data/IItemList;Lappeng/api/storage/data/IAEStack;Lappeng/api/storage/data/IAEStack;Lappeng/api/networking/security/BaseActionSource;Lappeng/api/storage/StorageChannel;)V";

    boolean hasOSC = false;

    public StackWatcherHostRepairer(String className) {
        super(className);
    }

    @Override
    public void visitMethod(String name, String desc) {
        if (methOSC.equals(name) && descOSCNew.equals(desc)) {
            hasOSC = true;
        }
    }

    @Override
    public void generateMethods(ClassVisitor cv) {
        if (!hasOSC) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methOSC, descOSCNew, null, null);
            mv.visitCode();
            Label labelEnd = new Label();
            // if (channel instanceof StorageChannel)
            mv.visitVarInsn(Opcodes.ALOAD, 5);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, "appeng/api/storage/StorageChannel");
            mv.visitJumpInsn(Opcodes.IFEQ, labelEnd);
            // {
            // StorageChannel c = (StorageChannel) channel;
            mv.visitVarInsn(Opcodes.ALOAD, 5);
            mv.visitTypeInsn(Opcodes.CHECKCAST, "appeng/api/storage/StorageChannel");
            mv.visitVarInsn(Opcodes.ASTORE, 7);
            // return this.onStackChange(o, fullStack, diffStack, src, c);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitVarInsn(Opcodes.ALOAD, 4);
            mv.visitVarInsn(Opcodes.ALOAD, 7);
            invokeMethod(mv, thisClass, methOSC, descOSCOld);
            // }
            mv.visitLabel(labelEnd);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        if ("appeng/api/networking/storage/IStackWatcherHost".equals(thisClass)) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methOSC, descOSCOld, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitVarInsn(Opcodes.ALOAD, 4);
            mv.visitVarInsn(Opcodes.ALOAD, 5);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, thisClass, methOSC, descOSCNew, true);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }
    
}
