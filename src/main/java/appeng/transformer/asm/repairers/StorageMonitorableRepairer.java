package appeng.transformer.asm.repairers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import appeng.transformer.asm.InterfaceMethodRepairer;

public class StorageMonitorableRepairer extends InterfaceMethodRepairer {

    static final String methGII = "getItemInventory";
    static final String descGII = "()Lappeng/api/storage/IMEMonitor;";
    static final String methGFI = "getFluidInventory";
    static final String descGFI = "()Lappeng/api/storage/IMEMonitor;";
    static final String methGI = "getInventory";
    static final String descGI = "(Lappeng/api/storage/IStorageChannel;)Lappeng/api/storage/IMEMonitor;";

    boolean hasGI = false;

    public StorageMonitorableRepairer(String className) {
        super(className);
    }

    @Override
    public void visitMethod(String name, String desc) {
        if (methGI.equals(name) && descGI.equals(desc)) {
            hasGI = true;
        }
    }

    @Override
    public void generateMethods(ClassVisitor cv) {
        if (!hasGI) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methGI, descGI, null, null);
            mv.visitCode();
            Label labelItems = new Label();
            Label labelFluids = new Label();
            // if (channel == StorageChannel.ITEMS)
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.GETSTATIC, "appeng/api/storage/StorageChannel", "ITEMS", "Lappeng/api/storage/StorageChannel;");
            mv.visitJumpInsn(Opcodes.IF_ACMPEQ, labelItems);
            // if (channel == StorageChannel.FLUIDS)
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.GETSTATIC, "appeng/api/storage/StorageChannel", "FLUIDS", "Lappeng/api/storage/StorageChannel;");
            mv.visitJumpInsn(Opcodes.IF_ACMPEQ, labelFluids);
            // return null;
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitInsn(Opcodes.ARETURN);
            // return getItemInventory();
            mv.visitLabel(labelItems);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            invokeMethod(mv, thisClass, methGII, descGII);
            mv.visitInsn(Opcodes.ARETURN);
            // return getItemInventory();
            mv.visitLabel(labelFluids);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            invokeMethod(mv, thisClass, methGFI, descGFI);
            mv.visitInsn(Opcodes.ARETURN);
            
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        if ("appeng/api/storage/IStorageMonitorable".equals(thisClass)) {
            {
                MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methGII, descGII, null, null);
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETSTATIC, "appeng/api/storage/StorageChannel", "ITEMS", "Lappeng/api/storage/StorageChannel;");
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, thisClass, methGI, descGI, true);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            {
                MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methGFI, descGFI, null, null);
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETSTATIC, "appeng/api/storage/StorageChannel", "FLUIDS", "Lappeng/api/storage/StorageChannel;");
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, thisClass, methGI, descGI, true);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }
    }
    
}
