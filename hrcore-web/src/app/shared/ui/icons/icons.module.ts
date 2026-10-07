// shared/ui/icons/icons.module.ts
import { NgModule } from '@angular/core';
import {
  LucideAngularModule,
  LayoutDashboard, Settings, Shield, Users, Key, Briefcase,
  CreditCard, ArrowRightLeft, DollarSign, RefreshCcw, Percent,
  Lock, ShieldAlert, BookOpen, Calendar, FileText, LogOut,
  Building, Receipt, Plus, X, Eye, PlayCircle, PauseCircle,
  EyeOff, Wand2, Clock,
  ClipboardList, ArrowLeft, ArrowRight, Search, Archive, Database, DatabaseSearch,
  Camera, UserCircle2, Save, RotateCcw, Trash2, ShieldCheck
} from 'lucide-angular';

@NgModule({
  imports: [
    LucideAngularModule.pick({
      LayoutDashboard, Settings, Shield, Users, Key, Briefcase,
      CreditCard, ArrowRightLeft, DollarSign, RefreshCcw, Percent,
      Lock, ShieldAlert, BookOpen, Calendar, FileText, LogOut,
      Building, Receipt, Plus, X, Eye, PlayCircle, PauseCircle, ClipboardList,
      EyeOff, Wand2, ArrowLeft, ArrowRight, Clock, Search, Archive, Database, DatabaseSearch,
      Camera, UserCircle2, Save, RotateCcw, Trash2, ShieldCheck
    })
  ],
  exports: [LucideAngularModule]
})
export class IconsModule { }
