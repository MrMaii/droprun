// Enhancement only: navigation, downloads and disclosure content work without JS.
if(!matchMedia('(prefers-reduced-motion: reduce)').matches&&'IntersectionObserver' in window){
 document.documentElement.classList.add('motion');
 const observer=new IntersectionObserver(entries=>{for(const e of entries)if(e.isIntersecting){e.target.classList.remove('waiting');observer.unobserve(e.target);}},{threshold:.08});
 for(const element of document.querySelectorAll('.reveal')){if(element.getBoundingClientRect().top>innerHeight)element.classList.add('waiting');observer.observe(element);}
}
