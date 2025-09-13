package appeng.transformer.asm.repairers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import appeng.transformer.asm.InterfaceMethodRepairer;

public class StorageGridRepairer extends InterfaceMethodRepairer {

    static final String methPAOSI = "postAlterationOfStoredItems";
    static final String descPAOSINew = "(Lappeng/api/storage/IStorageChannel;Ljava/lang/Iterable;Lappeng/api/networking/security/BaseActionSource;)V";
    static final String descPAOSIOld = "(Lappeng/api/storage/StorageChannel;Ljava/lang/Iterable;Lappeng/api/networking/security/BaseActionSource;)V";

    public StorageGridRepairer(String className) {
        super(className);
    }

    @Override
    public void visitMethod(String name, String desc) {
        
    }

    @Override
    public void generateMethods(ClassVisitor cv) {
        if ("appeng/api/networking/storage/IStorageGrid".equals(thisClass)) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methPAOSI, descPAOSIOld, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, thisClass, methPAOSI, descPAOSINew, true);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }
    
}
