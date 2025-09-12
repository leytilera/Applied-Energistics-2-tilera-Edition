package appeng.transformer.asm.repairers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import appeng.transformer.asm.InterfaceMethodRepairer;

public class CellRegistryRepairer extends InterfaceMethodRepairer {

    static final String methGCI = "getCellInventory";
    static final String descGCINew = "(Lnet/minecraft/item/ItemStack;Lappeng/api/storage/ISaveProvider;Lappeng/api/storage/IStorageChannel;)Lappeng/api/storage/IMEInventoryHandler;";
    static final String descGCIOld = "(Lnet/minecraft/item/ItemStack;Lappeng/api/storage/ISaveProvider;Lappeng/api/storage/StorageChannel;)Lappeng/api/storage/IMEInventoryHandler;";

    public CellRegistryRepairer(String className) {
        super(className);
    }

    @Override
    public void visitMethod(String name, String desc) {
        
    }

    @Override
    public void generateMethods(ClassVisitor cv) {
        if ("appeng/api/storage/ICellRegistry".equals(thisClass)) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methGCI, descGCIOld, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, thisClass, methGCI, descGCINew, true);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }
    
}
