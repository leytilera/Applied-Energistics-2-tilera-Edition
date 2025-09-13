package appeng.transformer.asm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import appeng.transformer.asm.repairers.GetChannelRepairer;
import appeng.transformer.asm.repairers.CellHandlerRepairer;
import appeng.transformer.asm.repairers.CellProviderRepairer;
import appeng.transformer.asm.repairers.CellRegistryRepairer;
import appeng.transformer.asm.repairers.ConstructorRepairer;
import appeng.transformer.asm.repairers.ExternalStorageHandlerRepairer;
import appeng.transformer.asm.repairers.ExternalStorageRegistryRepairer;
import appeng.transformer.asm.repairers.StackWatcherHostRepairer;
import appeng.transformer.asm.repairers.StorageGridRepairer;
import appeng.transformer.asm.repairers.StorageMonitorableRepairer;
import net.minecraft.launchwrapper.IClassTransformer;

public class StorageChannelTweaks implements IClassTransformer {

    Map<String, Function<String, InterfaceMethodRepairer>> repairers = new HashMap<>();
    Function<String, InterfaceMethodRepairer> cRepairer = ConstructorRepairer::new;
    Set<String> transformedClasses = new HashSet<>();

    public StorageChannelTweaks() {
        repairers.put("appeng/api/storage/ICellHandler", CellHandlerRepairer::new);
        repairers.put("appeng/api/storage/ICellProvider", CellProviderRepairer::new);
        repairers.put("appeng/api/storage/ICellRegistry", CellRegistryRepairer::new);
        repairers.put("appeng/api/storage/IExternalStorageHandler", ExternalStorageHandlerRepairer::new);
        repairers.put("appeng/api/storage/IExternalStorageRegistry", ExternalStorageRegistryRepairer::new);
        repairers.put("appeng/api/storage/IStorageMonitorable", StorageMonitorableRepairer::new);
        repairers.put("appeng/api/storage/IMEInventory", GetChannelRepairer::new);
        repairers.put("appeng/api/storage/data/IAEStack", GetChannelRepairer::new);
        repairers.put("appeng/api/networking/storage/IStorageGrid", StorageGridRepairer::new);
        repairers.put("appeng/api/networking/storage/IStackWatcherHost", StackWatcherHostRepairer::new);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        Map<Class<? extends InterfaceMethodRepairer>, InterfaceMethodRepairer> activeRepairers = new HashMap<>();

        ClassReader classReader = new ClassReader(basicClass);
        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5, classWriter) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
                if (transformedClasses.contains(name) || name.startsWith("appeng/transformer/asm")) {
                    super.visit(version, access, name, signature, superName, interfaces);
                } else if (name.equals("appeng/api/storage/StorageChannel")) {
                    String[] interf = new String[]{
                        "appeng/api/storage/channels/IItemStorageChannel",
                        "appeng/api/storage/channels/IFluidStorageChannel"
                    };
                    super.visit(version, access, name, signature, superName, interf);
                } else {
                    for(String interf : interfaces) {
                        if (repairers.containsKey(interf)) {
                            InterfaceMethodRepairer imr = repairers.get(interf).apply(name);
                            if (isInterface) imr.markInterface();
                            activeRepairers.put(imr.getClass(), imr);
                            break;
                        }
                    }
                    if (repairers.containsKey(name)) {
                        InterfaceMethodRepairer imr = repairers.get(name).apply(name);
                        if (isInterface) imr.markInterface();
                        activeRepairers.put(imr.getClass(), imr);
                    }
                    if (!isInterface) {
                        InterfaceMethodRepairer imr = cRepairer.apply(name);
                        activeRepairers.put(imr.getClass(), imr);
                    }
                    if (!activeRepairers.isEmpty()) {
                        transformedClasses.add(name);
                    } 
                    super.visit(version, access, name, signature, superName, interfaces);
                }
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                    String[] exceptions) {
                for (InterfaceMethodRepairer r : activeRepairers.values()) {
                    r.visitMethod(name, desc);
                }
                return super.visitMethod(access, name, desc, signature, exceptions);
            }

            @Override
            public void visitEnd() {
                for (InterfaceMethodRepairer r : activeRepairers.values()) {
                    r.generateMethods(this);
                }
                super.visitEnd();
            }
        };
        classReader.accept(classVisitor, 0);
        return classWriter.toByteArray();
    }
    
}
