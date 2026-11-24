import React, { Suspense, useEffect, useMemo, useState } from "react";
import { Canvas } from "@react-three/fiber";
import { OrbitControls, useGLTF, Environment, ContactShadows } from "@react-three/drei";

/**
 * INDRA Anime Hero Customizer
 * Expects public/models/indra-hero-starter.glb.
 * This starter GLB is a static modular visual prototype, not a rigged animated production model.
 */
const OUTFITS = [
  ["shadow", "Shadow Blaze"], ["white", "White Storm"],
  ["cyber", "Cyber Hunter"], ["gold", "Golden King"],
  ["purple", "Dark Phantom"], ["ninja", "Red Ninja"]
];
const HAIR = [["black","Black"],["white","White"],["red","Crimson"],["blue","Azure"]];
const ACCESSORIES = [["katana","Katana"],["mask","Ninja Mask"],["wings","Energy Wings"],["none","None"]];

function HeroAsset({ outfit, hair, accessory }) {
  const { scene } = useGLTF("/models/indra-hero-starter.glb");
  const model = useMemo(() => scene.clone(true), [scene]);

  useEffect(() => {
    model.traverse((obj) => {
      if (!obj.isMesh) return;
      const n = obj.name || "";
      if (n.startsWith("OUTFIT_")) obj.visible = n.startsWith(`OUTFIT_${outfit}_`);
      else if (n.startsWith("HAIR_")) obj.visible = n.startsWith(`HAIR_${hair}_`);
      else if (n.startsWith("ACCESSORY_")) {
        obj.visible = accessory === "none" ? false : n.startsWith(`ACCESSORY_${accessory}_`) || n === `ACCESSORY_${accessory}`;
      }
      obj.castShadow = true;
      obj.receiveShadow = true;
    });
  }, [model, outfit, hair, accessory]);

  return <primitive object={model} position={[0,-1,0]} />;
}

export default function IndraCharacterCustomizer({ onApply }) {
  const [outfit, setOutfit] = useState("shadow");
  const [hair, setHair] = useState("black");
  const [accessory, setAccessory] = useState("katana");
  const [tab, setTab] = useState("outfit");

  useEffect(() => {
    try {
      const saved = JSON.parse(localStorage.getItem("indra.hero.customization") || "null");
      if (saved) {
        setOutfit(saved.outfit || "shadow");
        setHair(saved.hair || "black");
        setAccessory(saved.accessory || "katana");
      }
    } catch (e) { console.warn("Could not load hero customization", e); }
  }, []);

  const save = () => {
    const data = { outfit, hair, accessory, version: 1 };
    localStorage.setItem("indra.hero.customization", JSON.stringify(data));
    onApply?.(data);
  };

  const options = tab === "outfit" ? OUTFITS : tab === "hair" ? HAIR : ACCESSORIES;
  const selected = tab === "outfit" ? outfit : tab === "hair" ? hair : accessory;
  const setter = tab === "outfit" ? setOutfit : tab === "hair" ? setHair : setAccessory;

  return (
    <div style={{ minHeight:"100vh", background:"#080d19", color:"#f4f6ff", padding:16, boxSizing:"border-box", fontFamily:"system-ui" }}>
      <header style={{ textAlign:"center", marginBottom:12 }}>
        <div style={{ color:"#ff4967", letterSpacing:3, fontSize:12 }}>INDRA • ANIME LIFE RP</div>
        <h2 style={{ margin:"5px 0" }}>HERO CUSTOMIZATION</h2>
        <small style={{ color:"#9daac3" }}>Rotate the character to inspect the costume</small>
      </header>
      <div style={{ height:"min(58vh,520px)", minHeight:320, border:"1px solid #293b5b", borderRadius:18, overflow:"hidden", background:"radial-gradient(circle at 50% 30%, #263c60, #090e1c 70%)" }}>
        <Canvas camera={{ position:[0,0.45,4.8], fov:34 }} shadows dpr={[1,1.5]}>
          <ambientLight intensity={1.15}/>
          <directionalLight position={[3,5,4]} intensity={2.2} castShadow/>
          <Suspense fallback={null}>
            <Environment preset="city"/>
            <HeroAsset outfit={outfit} hair={hair} accessory={accessory}/>
            <ContactShadows position={[0,-1,0]} opacity={0.45} scale={4} blur={2}/>
          </Suspense>
          <OrbitControls enablePan={false} minDistance={2.7} maxDistance={7} target={[0,0.05,0]}/>
        </Canvas>
      </div>
      <div style={{ display:"flex", gap:8, marginTop:14 }}>
        {["outfit","hair","accessory"].map(t => <button key={t} onClick={()=>setTab(t)} style={{ flex:1, padding:12, border:0, borderRadius:10, color:"white", background:tab===t?"#d72d4d":"#18243a", fontWeight:700 }}>{t.toUpperCase()}</button>)}
      </div>
      <div style={{ display:"grid", gridTemplateColumns:"repeat(2,minmax(0,1fr))", gap:9, marginTop:12 }}>
        {options.map(([id,label]) => <button key={id} onClick={()=>setter(id)} style={{ padding:13, borderRadius:10, border:selected===id?"2px solid #ff4967":"1px solid #33435f", color:"white", background:"#121d31", textAlign:"left" }}>{selected===id?"✓ ":""}{label}</button>)}
      </div>
      <div style={{ display:"flex", gap:9, marginTop:16 }}>
        <button onClick={()=>{setOutfit("shadow");setHair("black");setAccessory("katana");}} style={{ flex:1, padding:14, border:0, borderRadius:10, background:"#34415a", color:"white" }}>RESET</button>
        <button onClick={save} style={{ flex:2, padding:14, border:0, borderRadius:10, background:"#e63251", color:"white", fontWeight:800 }}>SAVE CHARACTER</button>
      </div>
    </div>
  );
}

useGLTF.preload("/models/indra-hero-starter.glb");
