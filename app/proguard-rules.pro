# Защита моделей данных и DAO
-keep class home.babbakappa.bktl.database.** { *; }
-keep class home.babbakappa.bktl.core.** { *; }

# Сохраняем аннотации, необходимые для работы Room в рантайме
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Сохраняем информацию о строках для понятных логов ошибок
-keepattributes SourceFile,LineNumberTable
