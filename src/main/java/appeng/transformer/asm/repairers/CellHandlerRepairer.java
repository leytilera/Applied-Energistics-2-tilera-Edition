package appeng.transformer.asm.repairers;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import appeng.transformer.asm.InterfaceMethodRepairer;

public class CellHandlerRepairer extends InterfaceMethodRepairer {

    static final String methGCI = "getCellInventory";
    static final String descGCINew = "(Lnet/minecraft/item/ItemStack;Lappeng/api/storage/ISaveProvider;Lappeng/api/storage/IStorageChannel;)Lappeng/api/storage/IMEInventoryHandler;";
    static final String descGCIOld = "(Lnet/minecraft/item/ItemStack;Lappeng/api/storage/ISaveProvider;Lappeng/api/storage/StorageChannel;)Lappeng/api/storage/IMEInventoryHandler;";
    static final String methOCG = "openChestGui";
    static final String descOCGNew = "(Lnet/minecraft/entity/player/EntityPlayer;Lappeng/api/implementations/tiles/IChestOrDrive;Lappeng/api/storage/ICellHandler;Lappeng/api/storage/IMEInventoryHandler;Lnet/minecraft/item/ItemStack;Lappeng/api/storage/IStorageChannel;)V";
    static final String descOCGOld = "(Lnet/minecraft/entity/player/EntityPlayer;Lappeng/api/implementations/tiles/IChestOrDrive;Lappeng/api/storage/ICellHandler;Lappeng/api/storage/IMEInventoryHandler;Lnet/minecraft/item/ItemStack;Lappeng/api/storage/StorageChannel;)V";

    boolean hasGCI = false;
    boolean hasOCG = false;

    public CellHandlerRepairer(String className) {
        super(className);
    }

    @Override
    public void visitMethod(String name, String desc) {
        if (methGCI.equals(name) && descGCINew.equals(desc)) {
            hasGCI = true;
        }
        if (methOCG.equals(name) && descOCGNew.equals(desc)) {
            hasOCG = true;
        }
    }

    @Override
    public void generateMethods(ClassVisitor cv) {
        if (!hasGCI && !isInterface) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methGCI, descGCINew, null, null);
            mv.visitCode();
            Label labelElse = new Label();
            Label labelEnd = new Label();
            // if (channel instanceof StorageChannel)
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, "appeng/api/storage/StorageChannel");
            mv.visitJumpInsn(Opcodes.IFEQ, labelElse);
            // {
            // StorageChannel c = (StorageChannel) channel;
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitTypeInsn(Opcodes.CHECKCAST, "appeng/api/storage/StorageChannel");
            mv.visitVarInsn(Opcodes.ASTORE, 4);
            // return this.getCellInventory(is, host, c);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 4);
            invokeMethod(mv, thisClass, methGCI, descGCIOld);
            // } else 
            mv.visitJumpInsn(Opcodes.GOTO, labelEnd);
            // {
            mv.visitLabel(labelElse);
            // return null;
            mv.visitInsn(Opcodes.ACONST_NULL);
            // }
            mv.visitLabel(labelEnd);
            // Actual return
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        if (!hasOCG) {
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methOCG, descOCGNew, null, null);
            mv.visitCode();
            Label labelEnd = new Label();
            // if (channel instanceof StorageChannel)
            mv.visitVarInsn(Opcodes.ALOAD, 6);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, "appeng/api/storage/StorageChannel");
            mv.visitJumpInsn(Opcodes.IFEQ, labelEnd);
            // {
            // StorageChannel c = (StorageChannel) channel;
            mv.visitVarInsn(Opcodes.ALOAD, 6);
            mv.visitTypeInsn(Opcodes.CHECKCAST, "appeng/api/storage/StorageChannel");
            mv.visitVarInsn(Opcodes.ASTORE, 7);
            // return this.getCellInventory(is, host, c);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            mv.visitVarInsn(Opcodes.ALOAD, 4);
            mv.visitVarInsn(Opcodes.ALOAD, 5);
            mv.visitVarInsn(Opcodes.ALOAD, 7);
            invokeMethod(mv, thisClass, methOCG, descOCGOld);
            // }
            mv.visitLabel(labelEnd);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        if ("appeng/api/storage/ICellHandler".equals(thisClass)) {
            {
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
            {
                MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, methOCG, descOCGOld, null, null);
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitVarInsn(Opcodes.ALOAD, 4);
                mv.visitVarInsn(Opcodes.ALOAD, 5);
                mv.visitVarInsn(Opcodes.ALOAD, 6);
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, thisClass, methOCG, descOCGNew, true);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }
    }
    
}
